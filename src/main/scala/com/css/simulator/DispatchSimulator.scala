package com.css.simulator

import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue

import com.css.simulator.model.{Courier, Order, OrderNotification}
import com.css.simulator.reader.OrderNotificationReader
import com.css.simulator.strategy.MatchStrategyStats
import com.css.simulator.worker._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object DispatchSimulator extends App with LazyLogging {
  val ONE_SECOND = 1 * 1000

  val simulatorConfig = SimulatorConfig.readSimulatorConfig()

  val orderQueue = new LinkedBlockingQueue[Order]
  val courierQueue = new LinkedBlockingQueue[Courier]
  val cloudKitchen = CloudKitchen(simulatorConfig, orderQueue, courierQueue)

  val matchStrategy = simulatorConfig.matchStrategy
  val matcher = Matcher(orderQueue, courierQueue, matchStrategy)

  val orderReceiptSpeed = simulatorConfig.orderReceiptSpeed

  val startTime = LocalDateTime.now()
  OrderNotificationReader.attemptReadOrdersFile(simulatorConfig.ordersFilePath) match {
    case Success(allOrders) => {
      performSimulation(allOrders)
    }
    case Failure(exception) => {
      logger.error("Simulation failed.", exception)
    }
  }
  val endTime = LocalDateTime.now()
  val simulationDuration = java.time.Duration.between(startTime, endTime)
  logger.info(s"StartTime = $startTime, EndTime = $endTime, Total SimulationDuration = $simulationDuration}")
  System.exit(0)

  def performSimulation(allOrderNotifications: Seq[OrderNotification]): Unit = {
    try {
      val readyOrdersProcessor = matcher.startMatchProcessing()
      val allOrdersAndCouriers = simulateOrderFlow(allOrderNotifications)

      val allOrders = allOrdersAndCouriers._1
      val failedOrders = Await.result(Future.sequence(allOrders), Duration.Inf).filter(_.isFailure)
      failedOrders.foreach(failedOrder => logger.error("Failed to cook order: ", failedOrder.failed.get))

      val allCouriers = allOrdersAndCouriers._2
      val failedCouriers = Await.result(Future.sequence(allCouriers), Duration.Inf).filter(_.isFailure)
      failedCouriers.foreach(failedCourier => logger.error("Failed to dispatch courier: ", failedCourier.failed.get))

      orderQueue.put(Order.DUMMY_ORDER)
      courierQueue.put(Courier.DUMMY_COURIER)

      Await.result(readyOrdersProcessor, Duration.Inf)

      MatchStrategyStats.printAllMatches(matchStrategy)
      logger.info(s"Results using match strategy: $matchStrategy")
      logger.info(s"Order receipt speed: ${simulatorConfig.orderReceiptSpeed}")
      logger.info(s"Cooking thread count: ${simulatorConfig.orderWorkerThreads}")
      logger.info(s"Courier dispatch thread count: ${simulatorConfig.orderWorkerThreads}")

      logger.info(s"Sizes: receivedOrders=${allOrders.size}, failedOrders=${failedOrders.size}, matchedOrders=${matchStrategy.getMatchedOrders.size}")
      logger.info(s"Sizes: dispatchedCouriers=${allCouriers.size}, failedCouriers=${failedCouriers.size}, matchedCouriers=${matchStrategy.getMatchedCouriers.size}")
      MatchStrategyStats.printStats(matchStrategy)
    } catch {
      case ex: Exception => logger.error("Simulation failed: ", ex)
    }
  }

  def simulateOrderFlow(allOrderNotifications: Seq[OrderNotification]): (Seq[Future[Try[Order]]], Seq[Future[Try[Courier]]]) = {
    def duplicateOrderNotifications(notifications: Seq[OrderNotification], times: Int) : Seq[OrderNotification] = {
      if(times == 0) {
        notifications
      } else {
        val duplicateNotifications =  notifications.map(orderNotification => orderNotification.copy(id = s"${orderNotification.id}_$times"))
        duplicateOrderNotifications(notifications ++ duplicateNotifications, times - 1)
      }
    }

    duplicateOrderNotifications(allOrderNotifications, 1).sliding(orderReceiptSpeed, orderReceiptSpeed).flatMap(orderBatch => {
      logger.info(s"Received new batch of ${orderBatch.size} orders.")

      val promisedOrdersAndCouriers = orderBatch.map(orderNotification => {
        val promisedOrder = cloudKitchen.cookOrder(orderNotification)
        val promisedCourier = cloudKitchen.dispatchCourier(orderNotification)
        logger.info(s"Dispatched courier and started cooking order for $orderNotification")

        Tuple2(promisedOrder, promisedCourier)
      })
      Thread.sleep(ONE_SECOND)

      promisedOrdersAndCouriers
    }).toSeq.unzip
  }
}

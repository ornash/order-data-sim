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
  logger.info(s"StartTime = $startTime, EndTime = $endTime, Total SimulationDuration = $simulationDuration")
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

      val matchStrategyStats = MatchStrategyStats(matchStrategy)
      matchStrategyStats.printAllMatches()
      logger.info(s"Results for processing orders file: ${simulatorConfig.ordersFilePath}")
      logger.info(s"Worker thread count for processing orders: ${simulatorConfig.orderWorkerThreads}")
      logger.info(s"worker thread count for dispatching couriers: ${simulatorConfig.courierDispatchThreads}")

      logger.info(s"Sizes: receivedOrders=${allOrders.size}, failedOrders=${failedOrders.size}, matchedOrders=${matchStrategy.getMatchedOrders.size}")
      logger.info(s"Sizes: dispatchedCouriers=${allCouriers.size}, failedCouriers=${failedCouriers.size}, matchedCouriers=${matchStrategy.getMatchedCouriers.size}")
      logger.info("")
      logger.info(s"Simulation order receipt speed: ${simulatorConfig.orderReceiptSpeed} per second")
      matchStrategyStats.printStats()
      logger.info("Simulation complete.")
    } catch {
      case ex: Exception => logger.error("Simulation failed: ", ex)
    }
  }

  /**
   * Simulates order flow based on provided allOrderNotifications by sending a batch of notifications per second.
   * The speed is determined by orderReceiptSpeed. Returns Future Order and Courier objects for all notifications.
   */
  def simulateOrderFlow(allOrderNotifications: Seq[OrderNotification]): (Seq[Future[Try[Order]]], Seq[Future[Try[Courier]]]) = {
    allOrderNotifications.sliding(orderReceiptSpeed, orderReceiptSpeed).flatMap(orderBatch => {
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

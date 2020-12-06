package com.css.simulator

import java.util.concurrent.{Executors, LinkedBlockingDeque}

import com.css.simulator.model.{Courier, Order, OrderNotification}
import com.css.simulator.reader.OrderNotificationReader
import com.css.simulator.worker._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn._
import scala.util.{Failure, Success}

object DispatchSimulator extends App with LazyLogging {
  val THREAD_POOL_SIZE = 32

  case class SimulatorConfig(ordersFilePath: String,
                             orderReceiptSpeed: Int,
                             totalWorkerThreads: Int = THREAD_POOL_SIZE,
                             matchStrategy: MatchStrategy)

  val simulatorConfig = readSimulatorConfig()

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(simulatorConfig.totalWorkerThreads))

  val orderQueue = new LinkedBlockingDeque[Order]
  val courierQueue = new LinkedBlockingDeque[Courier]
  val cloudKitchen = CloudKitchen(ec, orderQueue)
  val courierDispatcher = CourierDispatcher(ec, courierQueue)

  val startTime = System.currentTimeMillis()
  OrderNotificationReader.attemptReadOrdersFile(simulatorConfig.ordersFilePath) match {
    case Success(allOrders) => {
      startSimulation(allOrders)
    }
    case Failure(exception) => {
      logger.error("Simulation failed.", exception)
    }
  }

  def readSimulatorConfig(): SimulatorConfig = {
    var inputLine = readLine("Enter orders input file [./dispatch_orders.json]: ")
    val ordersFilePath = if(inputLine.isEmpty) "./dispatch_orders.json" else inputLine

    inputLine = readLine("Enter order receipt speed per second [2]: ")
    val orderReceiptSpeed = if(inputLine.isEmpty) 2 else inputLine.toInt

    inputLine = readLine(s"Enter worker thread count [$THREAD_POOL_SIZE]: ")
    val totalWorkerThreads = if(inputLine.isEmpty) THREAD_POOL_SIZE else inputLine.toInt

    inputLine = readLine(s"Enter 1 for FIFO match strategy or 2 for OrderId match strategy: [1]: ")
    val strategySelection = if(inputLine.isEmpty) 1 else inputLine.toInt

    SimulatorConfig(ordersFilePath, orderReceiptSpeed, totalWorkerThreads, if(strategySelection == 1) FifoMatchStrategy() else OrderIdMatchStrategy())
  }

  def startSimulation(allOrderNotifications: Seq[OrderNotification]): Unit = {
    val orderReceiptSpeed = simulatorConfig.orderReceiptSpeed

    val matcher = Matcher(ec, orderQueue, courierQueue, simulatorConfig.matchStrategy)
    val processor = Future {matcher.processCompletedOrders()}(ec)

    def duplicateOrderNotifications(notifications: Seq[OrderNotification], times: Int) : Seq[OrderNotification] = {
      if(times == 0) {
        notifications
      } else {
        val duplicateNotifications =  notifications.map(orderNotification => orderNotification.copy(id = s"${orderNotification.id}_$times"))
        duplicateOrderNotifications(notifications ++ duplicateNotifications, times - 1)
      }
    }

    val allOrdersAndCouriers = duplicateOrderNotifications(allOrderNotifications, 3).sliding(orderReceiptSpeed, orderReceiptSpeed).flatMap(orderBatch => {
      logger.info(s"Processing order batch: ${orderBatch.toString()}")

      val promisedOrdersAndCouriers = orderBatch.map(orderNotification => {
        val promisedOrder = cloudKitchen.cookOrder(orderNotification)
        val promisedCourier = courierDispatcher.dispatchCourier(orderNotification)

        Tuple2(promisedOrder, promisedCourier)
      })
      Thread.sleep(1000)

      promisedOrdersAndCouriers
    }).toSeq.unzip

    val allOrders = allOrdersAndCouriers._1
    val allCouriers = allOrdersAndCouriers._2
    Await.result(Future.sequence(allOrders), Duration.Inf)
    Await.result(Future.sequence(allCouriers), Duration.Inf)

    orderQueue.put(Order.DUMMY_ORDER)
    courierQueue.put(Courier.DUMMY_COURIER)

//    processor.onComplete {
//      case Success(_) => logger.info(s"Processor completed.")
//      case Failure(t) => logger.error(s"Processor failed", t)
//    }(ec)
    Await.result(processor, Duration.Inf)

    val matchStrategy = matcher.matchStrategy
    matchStrategy.getMatchedOrders().foreach(order => {
      logger.info(s"Expected prepDuration = ${order.prepDuration} for order $order")
    })

    matchStrategy.getMatchedCouriers().foreach(courier => {
      logger.info(s"Expected arrivalDelay = ${courier.arrivalDelayDuration} for courier $courier")
    })

    logger.info(s"Results Using match strategy: $matchStrategy")
    logger.info(s"Sizes: receivedOrders=${allOrders.size}, matchedOrders=${matchStrategy.getMatchedOrders.size}")
    logger.info(s"Sizes: receivedCouriers=${allCouriers.size}, matchedCouriers=${matchStrategy.getMatchedCouriers.size}")

    printStats("Order Receipt", matchStrategy.getMatchedOrders.map(_.receivedDuration().get))
    printStats("Expected Order Prep", matchStrategy.getMatchedOrders.map(_.prepDuration))
    printStats("Order Cooking", matchStrategy.getMatchedOrders.map(_.cookDuration().get))
    printStats("Order Wait", matchStrategy.getMatchedOrders.map(_.waitDuration().get))

    printStats("Expected Courier ArrivalDelay", matchStrategy.getMatchedCouriers.map(_.arrivalDelayDuration))
    printStats("Courier Dispatch", matchStrategy.getMatchedCouriers.map(_.dispatchDuration().get))
    printStats("Courier Wait", matchStrategy.getMatchedCouriers.map(_.waitDuration().get))
  }

  //TODO: write this to a csv for analysis
  def printStats(of: String, durations: Seq[java.time.Duration]): Unit = {
    val total = durations.size
    val avgMs = durations.map(_.toMillis).foldLeft(0L)(_ + _) / total
    val maxMs = durations.map(_.toMillis).max
    val minMs = durations.map(_.toMillis).min
    val medianMs = medianCalculator(durations.map(_.toMillis))
    val arrivalSpeed = simulatorConfig.orderReceiptSpeed
    val threadCount = simulatorConfig.totalWorkerThreads
    val indentString = " " * (30 - of.length)
    logger.info(s"$of Stats: $indentString ThreadCount= $threadCount, ArrivalSpeed=$arrivalSpeed, Total=$total, Avg=$avgMs, Median=$medianMs, Max=$maxMs, Min=$minMs")
  }

  def medianCalculator(seq: Seq[Long]): Long = {
    //In order if you are not sure that 'seq' is sorted
    val sortedSeq = seq.sortWith(_ < _)

    if (seq.size % 2 == 1) {
      sortedSeq(sortedSeq.size / 2)
    } else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      (up.last + down.head) / 2
    }
  }

  logger.info(s"StartTime = $startTime EndTime = ${System.currentTimeMillis()} Diff = ${System.currentTimeMillis() - startTime}")
  System.exit(0)
}


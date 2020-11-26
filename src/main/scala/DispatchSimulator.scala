import java.util.concurrent.{Executors, LinkedBlockingDeque}

import com.css.model.{Courier, Order, OrderNotification}
import com.css.reader.OrderNotificationReader
import com.css.worker.{CloudKitchen, CourierDispatcher, FifoMatcher}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn._
import scala.util.{Failure, Success}

object DispatchSimulator extends App with LazyLogging {
  case class SimulatorConfig(ordersFilePath: String, orderReceiptSpeed: Int)

  val THREAD_POOL_SIZE = 32

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE))

  val simulatorConfig = readSimulatorConfig()
  val orderQueue = new LinkedBlockingDeque[Order]
  val courierQueue = new LinkedBlockingDeque[Courier]
  val cloudKitchen = CloudKitchen(ec, orderQueue)
  val courierDispatcher = CourierDispatcher(ec, courierQueue)

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

    SimulatorConfig(ordersFilePath, orderReceiptSpeed)
  }

  def startSimulation(allOrderNotifications: Seq[OrderNotification]): Unit = {
    val orderReceiptSpeed = simulatorConfig.orderReceiptSpeed

    val allOrdersAndCouriers = allOrderNotifications.sliding(orderReceiptSpeed, orderReceiptSpeed).flatMap(orderBatch => {
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

    orderQueue.put(Order.LAST_ORDER)
    courierQueue.put(Courier.LAST_COURIER)

    val fifoMatcher = FifoMatcher(ec, orderQueue, courierQueue)
    fifoMatcher.processCompletedOrders()

    fifoMatcher.matchedOrders.foreach(order => {
      logger.info(s"Wait = ${order.waitDuration()} for order $order")
    })

    fifoMatcher.matchedCouriers.foreach(courier => {
      logger.info(s"Wait = ${courier.waitDuration()} for courier $courier")
    })
  }
}


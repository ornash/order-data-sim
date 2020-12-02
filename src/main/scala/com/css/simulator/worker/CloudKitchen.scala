package com.css.simulator.worker

import java.util.concurrent.LinkedBlockingDeque

import com.css.simulator.model.{Order, OrderNotification}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

case class CloudKitchen(ec: ExecutionContext, orderQueue: LinkedBlockingDeque[Order]) extends LazyLogging {

  def cookOrder(orderNotification: OrderNotification): Future[Order] = {
    val receivedOrder = Order.fromOrderNotification(orderNotification)
    val startTime = System.currentTimeMillis()
    var printOrder = Option.empty[Order]

    val chef = Future {
      val cookingOrder  = Order.startCooking(receivedOrder)
      Thread.sleep(receivedOrder.prepDuration.toMillis)
      val readyOrder = Order.readyForPickup(cookingOrder)
      //blocking operation
      orderQueue.put(readyOrder)
      printOrder = Some(readyOrder)
      logger.info(s"Chef cooked order[${System.currentTimeMillis() - startTime}]: ${printOrder.get}")
      readyOrder
    }(ec)

//    chef.onComplete {
//      case Success(_) => logger.info(s"Chef completed order[${System.currentTimeMillis() - startTime}]: ${printOrder.get}")
//      case Failure(t) => logger.error(s"Chef failed to complete order: $receivedOrder", t)
//    }(ec)

    chef
  }
}

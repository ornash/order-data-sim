package com.css.worker

import java.util.concurrent.LinkedBlockingDeque

import com.css.model.{Order, OrderNotification}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class CloudKitchen(ec: ExecutionContext, orderQueue: LinkedBlockingDeque[Order]) extends LazyLogging {

  def cookOrder(orderNotification: OrderNotification): Future[Order] = {
    val receivedOrder = Order.fromOrderNotification(orderNotification)

    val chef = Future {
      val cookingOrder  = Order.startCooking(receivedOrder)
      Thread.sleep(receivedOrder.prepDuration.toMillis)
      val readyOrder = Order.readyForPickup(cookingOrder)
      //blocking operation
      orderQueue.put(readyOrder)
      readyOrder
    }(ec)

    chef.onComplete {
      case Success(_) => logger.info(s"Chef completed order: $receivedOrder")
      case Failure(t) => logger.error(s"Chef failed to complete order: $receivedOrder", t)
    }(ec)

    chef
  }
}

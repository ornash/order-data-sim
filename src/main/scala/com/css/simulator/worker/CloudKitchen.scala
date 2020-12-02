package com.css.simulator.worker

import java.util.concurrent.LinkedBlockingDeque

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Order, OrderNotification}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class CloudKitchen(ec: ExecutionContext, orderQueue: LinkedBlockingDeque[Order]) extends LazyLogging {

  def cookOrder(orderNotification: OrderNotification): Future[Order] = {
    val receivedOrder = Order.fromOrderNotification(orderNotification)

    val chef = Future {
      Order.startCooking(receivedOrder) match {
        case Failure(ex) => throw SimulatorException(s"Failed to start cooking order: $receivedOrder", ex)
        case Success(cookingOrder) => {
          Thread.sleep(cookingOrder.prepDuration.toMillis)

          Order.readyForPickup(cookingOrder) match {
            case Failure(ex) => throw SimulatorException(s"Failed to make order ready for pickup: $cookingOrder", ex)
            case Success(readyOrder) => {
              //blocking operation
              orderQueue.put(readyOrder)
              logger.info(s"Chef cooked order: $readyOrder")
              readyOrder
            }
          }
        }
      }
    }(ec)

//    chef.onComplete {
//      case Success(readyOrder) => logger.info(s"Chef completed order: $readyOrder")
//      case Failure(t) => logger.error(s"Chef failed to complete order: $receivedOrder", t)
//    }(ec)

    chef
  }
}

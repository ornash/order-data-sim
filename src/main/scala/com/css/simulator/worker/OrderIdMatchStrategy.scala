package com.css.simulator.worker

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.util.{Failure, Success}

case class OrderIdMatchStrategy() extends MatchStrategy with LazyLogging {

  val cookedOrderMap = mutable.Map.empty[String, Order]
  val arrivedCourierMap = mutable.Map.empty[String, Courier]

  override def processBatch(ordersBatch: Seq[Order], couriersBatch: Seq[Courier]): Unit = {
    ordersBatch.foreach(cookedOrder => {
      val waitingCourier = arrivedCourierMap.remove(cookedOrder.id)
      if (waitingCourier.isDefined) {
        Courier.matched(waitingCourier.get) match {
          case Failure(exception) => throw exception
          case Success(matchedCourier) => matchedCouriers.addOne(matchedCourier)
        }

        Order.pickup(cookedOrder) match {
          case Failure(exception) => throw exception
          case Success(matchedOrder) => matchedOrders.addOne(matchedOrder)
        }
      } else {
        cookedOrderMap.put(cookedOrder.id, cookedOrder)
      }
    })

    couriersBatch.foreach(arrivedCourier => {
      val cookedOrder = cookedOrderMap.remove(arrivedCourier.orderId.get)
      if (cookedOrder.isDefined) {
        Courier.matched(arrivedCourier) match {
          case Failure(exception) => throw exception
          case Success(matchedCourier) => matchedCouriers.addOne(matchedCourier)
        }

        Order.pickup(cookedOrder.get) match {
          case Failure(exception) => throw exception
          case Success(matchedOrder) => matchedOrders.addOne(matchedOrder)
        }
      } else {
        arrivedCourierMap.put(arrivedCourier.orderId.get, arrivedCourier)
      }
    })
  }
}
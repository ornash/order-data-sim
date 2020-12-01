package com.css.simulator.worker

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

case class OrderIdMatchStrategy() extends MatchStrategy with LazyLogging {

  val cookedOrderMap = mutable.Map.empty[String, Order]
  val arrivedCourierMap = mutable.Map.empty[String, Courier]

  override def processBatch(ordersBatch: Seq[Order], couriersBatch: Seq[Courier]): Unit = {
    ordersBatch.foreach(cookedOrder => {
      val waitingCourier = arrivedCourierMap.remove(cookedOrder.id)
      if (waitingCourier.isDefined) {
        matchedCouriers.addOne(Courier.matched(waitingCourier.get))
        matchedOrders.addOne(Order.pickup(cookedOrder))
      } else {
        cookedOrderMap.put(cookedOrder.id, cookedOrder)
      }
    })

    couriersBatch.foreach(arrivedCourier => {
      val cookedOrder = cookedOrderMap.remove(arrivedCourier.orderId.get)
      if (cookedOrder.isDefined) {
        matchedOrders.addOne(Order.pickup(cookedOrder.get))
        matchedCouriers.addOne(Courier.matched(arrivedCourier))
      } else {
        arrivedCourierMap.put(arrivedCourier.orderId.get, arrivedCourier)
      }
    })
  }
}
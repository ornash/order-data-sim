package com.css.simulator.strategy

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.util.{Failure, Success}

case class OrderIdMatchStrategy() extends MatchStrategy {

  private val cookedOrderMap = mutable.Map.empty[String, Order]
  private val arrivedCourierMap = mutable.Map.empty[String, Courier]

  override def matchCookedOrders(cookedOrdersBatch: Seq[Order]): Unit = {
    cookedOrdersBatch.foreach(cookedOrder => {
      val waitingCourier = arrivedCourierMap.remove(cookedOrder.id)
      if (waitingCourier.isDefined) {
        matchOrderWithCourier(cookedOrder, waitingCourier.get) match {
          case Failure(exception) => throw exception
          case Success(_) => {}
        }
      } else {
        cookedOrderMap.put(cookedOrder.id, cookedOrder)
      }
    })
  }

  override def matchArrivedCouriers(arrivedCouriersBatch: Seq[Courier]): Unit = {
    arrivedCouriersBatch.foreach(arrivedCourier => {
      val cookedOrder = cookedOrderMap.remove(arrivedCourier.orderId.get)
      if (cookedOrder.isDefined) {
        matchOrderWithCourier(cookedOrder.get, arrivedCourier) match {
          case Failure(exception) => throw exception
          case Success(_) => {}
        }
      } else {
        arrivedCourierMap.put(arrivedCourier.orderId.get, arrivedCourier)
      }
    })
  }
}
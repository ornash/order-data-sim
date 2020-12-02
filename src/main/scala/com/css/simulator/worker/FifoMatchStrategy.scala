package com.css.simulator.worker

import java.time.LocalDateTime

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

case class FifoMatchStrategy() extends MatchStrategy with LazyLogging {
  implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  val cookedOrders = mutable.ArrayBuffer.empty[Order]
  val arrivedCouriers = mutable.PriorityQueue()(Ordering.by[Courier, LocalDateTime](_.arrivalTime())(dateTimeOrdering))

  override def processBatch(ordersBatch: Seq[Order], couriersBatch: Seq[Courier]): Unit = {
    ordersBatch.foreach(cookedOrder => {
      cookedOrders.addOne(cookedOrder)
    })

    couriersBatch.foreach(arrivedCourier => {
      arrivedCouriers.addOne(arrivedCourier)
    })

    val cookedOrdersToBeRemoved = mutable.ArrayBuffer.empty[Order]

    cookedOrders.foreach(cookedOrder => {
      Try(arrivedCouriers.dequeue()) match {
        case Success(arrivedCourier) => {
          //FIXME which order did this match with
          matchedCouriers.addOne(Courier.matched(arrivedCourier))
          matchedOrders.addOne(Order.pickup(cookedOrder))
          cookedOrdersToBeRemoved.addOne(cookedOrder)
        }
        case Failure(exception) => {}
      }
    })

    cookedOrders.subtractAll(cookedOrdersToBeRemoved)
  }

}
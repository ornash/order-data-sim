package com.css.simulator.strategy

import java.time.LocalDateTime

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.util.{Failure, Success}

case class FifoMatchStrategy() extends MatchStrategy {
  implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  private val cookedOrders = mutable.ArrayBuffer.empty[Order]
  private val arrivedCouriers = mutable.PriorityQueue()(Ordering.by[Courier, LocalDateTime](_.arrivalTime().get)(dateTimeOrdering))

  private def applyFifoMatchStrategy(): Unit = {
    val cookedOrdersToBeRemoved = mutable.ArrayBuffer.empty[Order]

    cookedOrders.foreach(cookedOrder => {
      if(!arrivedCouriers.isEmpty) {
        val arrivedCourier = arrivedCouriers.dequeue()
        matchOrderWithCourier(cookedOrder, arrivedCourier) match {
          case Failure(exception) => throw exception
          case Success(_) => cookedOrdersToBeRemoved.addOne(cookedOrder)
        }
      }
    })

    cookedOrders.subtractAll(cookedOrdersToBeRemoved)
  }

  override def matchCookedOrders(cookedOrdersBatch: Seq[Order]): Unit = {
    cookedOrdersBatch.foreach(cookedOrder => {
      cookedOrders.addOne(cookedOrder)
    })
    applyFifoMatchStrategy()
  }

  override def matchArrivedCouriers(arrivedCouriersBatch: Seq[Courier]): Unit = {
    arrivedCouriersBatch.foreach(arrivedCourier => {
      arrivedCouriers.addOne(arrivedCourier)
    })
    applyFifoMatchStrategy()
  }
}
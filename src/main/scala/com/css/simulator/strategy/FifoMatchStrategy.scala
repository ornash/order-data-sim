package com.css.simulator.strategy

import java.time.LocalDateTime

import com.css.simulator.model.{Courier, Order, OrderAndCourier}

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
 * Match orders with couriers in FIFO order. This class is not thread-safe.
 *
 * FIFO :- A courier picks up the next available order upon arrival. If there are multiple orders available, pick up an
 * arbitrary order. If there are no available orders, couriers wait for the next available one. When there are multiple
 * couriers waiting, the next available order is assigned to the earliest arrived courier.
 */
case class FifoMatchStrategy() extends MatchStrategy {
  implicit def ascDateTimeOrdering: Ordering[LocalDateTime] = Ordering.ordered[LocalDateTime].reverse

  private val readyOrders = mutable.ArrayBuffer.empty[Order]

  //PriorityQueue of Courier organized by their arrivalInstant() in ascending order.
  private val arrivedCouriers = mutable.PriorityQueue()(Ordering.by[Courier, LocalDateTime](_.arrivalInstant().get)(ascDateTimeOrdering))

  private def applyFifoMatchStrategy(): Seq[OrderAndCourier] = {
    val readyOrdersToBeRemoved = mutable.ArrayBuffer.empty[Order]
    val fifoMatches = mutable.ArrayBuffer.empty[OrderAndCourier]

    readyOrders.foreach(readyOrder => {
      if (!arrivedCouriers.isEmpty) {
        //dequeue courier that arrived earliest and match it with this cooked/ready order.
        val arrivedCourier = arrivedCouriers.dequeue()
        matchOrderWithCourier(readyOrder, arrivedCourier) match {
          case Failure(exception) => throw exception
          case Success(matchedOrderAndCourier) => {
            fifoMatches.addOne(matchedOrderAndCourier)
            readyOrdersToBeRemoved.addOne(readyOrder)
          }
        }
      }
    })

    readyOrders.subtractAll(readyOrdersToBeRemoved)
    fifoMatches.toSeq
  }

  override def matchReadyOrders(readyOrdersBatch: Seq[Order]): Seq[OrderAndCourier] = {
    readyOrders.addAll(readyOrdersBatch)
    applyFifoMatchStrategy()
  }

  override def matchArrivedCouriers(arrivedCouriersBatch: Seq[Courier]): Seq[OrderAndCourier] = {
    arrivedCouriers.enqueue(arrivedCouriersBatch : _*)
    applyFifoMatchStrategy()
  }
}
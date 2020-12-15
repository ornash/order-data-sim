package com.css.simulator.strategy

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order, OrderAndCourier}

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
 * Match orders with couriers based on order-id. This class is not thread-safe.
 *
 * In this strategy, a courier is dispatched for a specific order and may only pick up that order.
 */
case class OrderIdMatchStrategy() extends MatchStrategy {

  //order-id to Order map
  private val readyOrderMap = mutable.Map.empty[String, Order]
  //order-id to Courier map
  private val arrivedCourierMap = mutable.Map.empty[String, Courier]

  override def matchReadyOrders(readyOrdersBatch: Seq[Order]): Seq[OrderAndCourier] = {
    val orderIdMatches = mutable.ArrayBuffer.empty[OrderAndCourier]

    readyOrdersBatch.map(readyOrder => {
      val waitingCourier = arrivedCourierMap.remove(readyOrder.id)
      if (waitingCourier.isDefined) { //match this ready/ready order with waiting courier
        matchOrderWithCourier(readyOrder, waitingCourier.get) match {
          case Failure(exception) => throw exception
          case Success(matchedOrderAndCourier) => orderIdMatches.addOne(matchedOrderAndCourier)
        }
      } else {
        readyOrderMap.put(readyOrder.id, readyOrder) //wait for courier
      }
    })

    orderIdMatches.toSeq
  }

  override def matchArrivedCouriers(arrivedCouriersBatch: Seq[Courier]): Seq[OrderAndCourier] = {
    val orderIdMatches = mutable.ArrayBuffer.empty[OrderAndCourier]

    arrivedCouriersBatch.foreach(arrivedCourier => {
      if(arrivedCourier.orderId.isEmpty) {
        val errorMsg = s"Failed to match arrived courier $arrivedCourier because it doesnt have order-id."
        logger.error(errorMsg)
        throw SimulatorException(errorMsg)
      }

      val readyOrder = readyOrderMap.remove(arrivedCourier.orderId.get)
      if (readyOrder.isDefined) { //match this arrived courier with cooked/ready order
        matchOrderWithCourier(readyOrder.get, arrivedCourier) match {
          case Failure(exception) => throw exception
          case Success(matchedOrderAndCourier) => orderIdMatches.addOne(matchedOrderAndCourier)
        }
      } else {
        arrivedCourierMap.put(arrivedCourier.orderId.get, arrivedCourier) //wait for order
      }
    })

    orderIdMatches.toSeq
  }
}
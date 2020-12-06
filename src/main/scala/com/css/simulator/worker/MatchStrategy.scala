package com.css.simulator.worker

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

trait MatchStrategy {
  case class OrderAndCourier(order: Order, courier: Courier)

  private val matchedOrderAndCouriers = mutable.ArrayBuffer.empty[OrderAndCourier]

  def getMatchedOrders(): Seq[Order] = {
    matchedOrderAndCouriers.map(_.order).toSeq
  }

  def getMatchedCouriers(): Seq[Courier] = {
    matchedOrderAndCouriers.map(_.courier).toSeq
  }

  def getMatchedOrderAndCouriers(): Seq[OrderAndCourier] = {
    matchedOrderAndCouriers.toSeq
  }

  def matchOrderWithCourier(cookedOrder: Order, arrivedCourier: Courier): Try[OrderAndCourier] = Try {
    Order.pickup(cookedOrder) match {
      case Failure(ex) => throw SimulatorException(s"Failed to match cooked order $cookedOrder with arrived courier $arrivedCourier", ex)

      case Success(pickupOrder) => {
        Courier.matched(arrivedCourier, pickupOrder.id) match {
          case Failure(ex) => throw SimulatorException(s"Failed to match pickup order $pickupOrder with arrived courier $arrivedCourier", ex)

          case Success(matchedCourier) => {
            val matchedOrderAndCourier = OrderAndCourier(pickupOrder, matchedCourier)
            matchedOrderAndCouriers.addOne(matchedOrderAndCourier)
            matchedOrderAndCourier
          }
        }
      }
    }
  }

  def matchCookedOrders(cookedOrders: Seq[Order])

  def matchArrivedCouriers(arrivedCouriers: Seq[Courier])
}

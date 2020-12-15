package com.css.simulator.strategy

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order, OrderAndCourier}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * Interface for matching orders with couriers. This interface is not thread-safe.
 *
 * Clients should invoke `matchreadyOrders` and `matchArrivedCouriers` methods to trigger implemented match strategy
 * based on available orders or couriers ready to be matched.
 *
 * Implementations should define `matchreadyOrders` and `matchArrivedCouriers` methods and call
 * `matchOrderWithCourier` to indicate a match.
 */
trait MatchStrategy extends LazyLogging {

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

  /**
   * Tries to match give ready order with the given arrived courier. If successful, the match is saved for tracking.
   */
  protected def matchOrderWithCourier(readyOrder: Order, arrivedCourier: Courier): Try[OrderAndCourier] = Try {
    Order.pickup(readyOrder) match {
      case Failure(ex) => {
        val errorMsg = s"Failed to match ready order $readyOrder with arrived courier $arrivedCourier"
        logger.error(errorMsg, ex)
        throw SimulatorException(errorMsg, ex)
      }

      case Success(pickupOrder) => {
        Courier.matched(arrivedCourier, pickupOrder.id) match {
          case Failure(ex) => {
            val errorMsg = s"Failed to match pickup order $pickupOrder with arrived courier $arrivedCourier"
            logger.error(errorMsg, ex)
            throw SimulatorException(errorMsg, ex)
          }

          case Success(matchedCourier) => {
            val matchedOrderAndCourier = OrderAndCourier(pickupOrder, matchedCourier)
            logger.info(s"Matched order-id: ${pickupOrder.id} with courier.")
            matchedOrderAndCouriers.addOne(matchedOrderAndCourier)
            matchedOrderAndCourier
          }
        }
      }
    }
  }

  /**
   * Matches given readyOrders with arrived couriers waiting for ready orders, if any, based on the match strategy.
   * @return Collection of matched orders and couriers in this attempt, could be empty.
   */
  def matchReadyOrders(readyOrders: Seq[Order]): Seq[OrderAndCourier]

  /**
   * Matches given arrivedCouriers with ready orders waiting for couriers, if any, based on the match strategy.
   * @return Collection of matched orders and couriers in this attempt, could be empty.
   */
  def matchArrivedCouriers(arrivedCouriers: Seq[Courier]): Seq[OrderAndCourier]
}

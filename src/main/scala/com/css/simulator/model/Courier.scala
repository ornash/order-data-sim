package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.{Failure, Success, Try}

/**
 * Represents a courier in its current status.
 *
 * @param orderId         Optional order-id the courier is supposed to deliver.
 *                        If the courier is matched to an order then orderId is the id of matched order.
 * @param courierId       Optional id of courier
 * @param transitDuration guaranteed time duration between receipt of dispatch by courier and its arrival at CloudKitchen.
 * @param currentStatus   current status of courier.
 */
case class Courier(orderId: Option[String] = None,
                   courierId: String = "",
                   transitDuration: Duration,
                   currentStatus: CourierStatus) {

  /**
   * Tries to transform this courier from current state to a new state.
   */
  def transform(newStatusType: CourierStatusType): Try[Courier] = Try {
    currentStatus.transform(newStatusType) match {
      case Success(newStatus) => this.copy(currentStatus = newStatus)
      case Failure(exception) => throw SimulatorException(s"Cannot transform Courier to $newStatusType", exception)
    }
  }

  /**
   * @return time instant at which the courier actually arrived.
   */
  def arrivalInstant(): Option[LocalDateTime] = {
    currentStatus.findCourierStatus(ARRIVED) match {
      case Some(arrivedCourierStatus) => Some(arrivedCourierStatus.startTime)
      case None => None
    }
  }

  /**
   * @return actual duration in dispatch state between receipt of dispatch by courier and its arrival at CloudKitchen.
   *         Should be greater than or equal to transitDuration.
   */
  def dispatchDuration(): Option[Duration] = { durationInStatus(DISPATCHED) }

  /**
   * @return duration spent waiting between arrival of courier and its match with an [[com.css.simulator.model.Order]]
   */
  def waitDuration(): Option[Duration] = { durationInStatus(ARRIVED) }

  private def durationInStatus(expectedStatusType: CourierStatusType): Option[Duration] = {
    currentStatus.findCourierStatus(expectedStatusType) match {
      case Some(expectedCourierStatus) => expectedCourierStatus.durationInStatus
      case None => None
    }
  }
}

object Courier {
  private val dummy = "dummy"
  val DUMMY_COURIER = Courier.dispatchNewCourier(Option(dummy), dummy, Duration.ofSeconds(0))

  def dispatchNewCourier(orderId: Option[String] = None,
                         courierId: String = "",
                         transitDuration: Duration): Courier = {
    Courier(orderId, courierId, transitDuration, CourierStatus(DISPATCHED))
  }

  /**
   * Makes a dispatched courier arrive at CloudKitchen. The arrival time is the time instant at which this method was invoked.
   */
  def arrived(dispatchedCourier: Courier): Try[Courier] = {
    dispatchedCourier.transform(ARRIVED)
  }

  /**
   * Matches an arrived courier with the order it was dispatched for. The match time is the time instant at which this method was invoked.
   */
  def matched(arrivedCourier: Courier): Try[Courier] = {
    arrivedCourier.orderId match {
      case Some(_) => arrivedCourier.transform(MATCHED)
      case None => Failure(SimulatorException(s"orderId is unavailable, courier $arrivedCourier cannot be matched."))
    }
  }

  /**
   * Matches an arrived courier with given order-id. The match time is the time instant at which this method was invoked.
   * The match will fail if given order-id doesnt match the expected order-id this courier was dispatched for.
   */
  def matched(arrivedCourier: Courier, newOrderId: String): Try[Courier] = {
    arrivedCourier.orderId match {
      case Some(expectedOrderId) => {
        if(expectedOrderId.equals(newOrderId)) {
          arrivedCourier.transform(MATCHED)
        } else {
          Failure(SimulatorException(s"newOrderId: $newOrderId doesn't match expected orderId for courier $arrivedCourier"))
        }
      }
      case None => arrivedCourier.copy(Some(newOrderId)).transform(MATCHED)
    }
  }

  /**
   * Delivers matched order. The delivery time is the time instant at which this method was invoked.
   */
  def deliver(matchedCourier: Courier): Try[Courier] = {
    matchedCourier.transform(HAS_DELIVERED)
  }
}


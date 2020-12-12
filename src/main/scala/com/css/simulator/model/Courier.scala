package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.{Failure, Random, Success, Try}

case class Courier(orderId: Option[String] = None,
                   courierId: String = "",
                   arrivalDelayDuration: Duration,
                   currentStatus: CourierStatus) {

  def transform(newStatusType: CourierStatusType): Try[Courier] = Try {
    currentStatus.transform(newStatusType) match {
      case Success(newStatus) => this.copy(currentStatus = newStatus)
      case Failure(exception) => throw SimulatorException(s"Cannot transform Courier to $newStatusType", exception)
    }
  }

  def arrivalTime(): Option[LocalDateTime] = {
    currentStatus.findCourierStatus(ARRIVED) match {
      case Some(arrivedCourierStatus) => Some(arrivedCourierStatus.startTime)
      case None => None
    }
  }

  def dispatchDuration(): Option[Duration] = {
    durationInStatus(DISPATCHED)
  }

  def waitDuration(): Option[Duration] = {
    durationInStatus(ARRIVED)
  }

  private def durationInStatus(expectedStatusType: CourierStatusType): Option[Duration] = {
    currentStatus.findCourierStatus(expectedStatusType) match {
      case Some(expectedCourierStatus) => Some(expectedCourierStatus.durationInStatus)
      case None => None
    }
  }
}

object Courier {
  val DUMMY_COURIER = Courier.dispatchNewCourier(Option("dummy"), "dummy")

  //FIXME: should duration be Random and should Random range be configurable
  def dispatchNewCourier(orderId: Option[String] = None,
                         courierId: String = "",
                         arrivalDelayDuration: Duration = Duration.ofSeconds(Random.between(3, 16))): Courier = {
    Courier(orderId, courierId, arrivalDelayDuration, CourierStatus(DISPATCHED))
  }

  def arrived(dispatchedCourier: Courier):  Try[Courier] = {
    dispatchedCourier.transform(ARRIVED)
  }

  def matched(arrivedCourier: Courier):  Try[Courier] = {
    arrivedCourier.transform(MATCHED)
  }

  def matched(arrivedCourier: Courier, newOrderId: String):  Try[Courier] = {
    arrivedCourier.copy(Some(newOrderId)).transform(MATCHED)
  }

  def deliver(matchedCourier: Courier): Try[Courier] = {
    matchedCourier.transform(HAS_DELIVERED)
  }
}


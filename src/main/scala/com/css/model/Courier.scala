package com.css.model

import java.time.{Duration, LocalDateTime}

import com.css.exception.SimulatorException

import scala.util.Random

case class Courier(orderId: Option[String] = None,
                   courierId: String = "",
                   arrivalDelayDuration: Duration,
                   currentStatus: CourierStatus,
                   previousStatuses: Seq[CourierStatus] = Seq.empty) {

  def isArrived() : Boolean = {
    currentStatus.statusType == ARRIVED
  }

  def isMatcheed() : Boolean = {
    currentStatus.statusType == MATCHED
  }

  def hasDelivered() : Boolean = {
    currentStatus.statusType == HAS_DELIVERED
  }

  def arrivalTime(): LocalDateTime = {
    if(currentStatus.statusType == ARRIVED) {
      currentStatus.startTime
    } else {
      val arrivedStatus = previousStatuses.find(ARRIVED == _.statusType)
      if(arrivedStatus.isEmpty) {
        throw SimulatorException(s"Cannot calculate arrival time of Courier $this, it hasn't arrived.")
      }
      arrivedStatus.get.startTime
    }
  }

  def waitDuration(): Duration = {
    val currentStatusType = currentStatus.statusType
    if(currentStatusType != MATCHED && currentStatusType != HAS_DELIVERED) {
      throw SimulatorException(s"Cannot calculate wait duration of Courier $this, it hasn't been matched or delivered.")
    }

    val arrivedStatus = previousStatuses.find(ARRIVED == _.statusType)
    if(arrivedStatus.isEmpty) {
      throw SimulatorException(s"Cannot calculate wait duration of Courier $this, it didn't enter arrived state.")
    }

    arrivedStatus.get.durationInStatus
  }
}

object Courier {
  //FIXME
  val LAST_COURIER = Courier.dispatchNewCourier(Option("last"), "last")

  def dispatchNewCourier(orderId: Option[String] = None,
                         courierId: String = "",
                         arrivalDelayDuration: Duration = Duration.ofSeconds(Random.between(3, 16))): Courier = {
    Courier(orderId, courierId, arrivalDelayDuration, CourierStatus(DISPATCHED))
  }

  def arrived(dispatchedCourier: Courier): Courier = {
    changeCourierStatus(dispatchedCourier, DISPATCHED, ARRIVED)
  }

  def matched(arrivedCourier: Courier): Courier = {
    changeCourierStatus(arrivedCourier, ARRIVED, MATCHED)
  }

  def deliver(matchedCourier: Courier): Courier = {
    changeCourierStatus(matchedCourier, MATCHED, HAS_DELIVERED)
  }

  private def changeCourierStatus(existingCourier: Courier,
                                  expectedCurrentStatusType: CourierStatusType,
                                  newStatusType: CourierStatusType): Courier = {
    val currentStatus = existingCourier.currentStatus
    if(currentStatus.statusType != expectedCurrentStatusType) {
      throw SimulatorException(s"Cannot change Courier status for $existingCourier, Courier is not : $expectedCurrentStatusType")
    }

    val endCurrentStatus = currentStatus.copy(endTime = Some(LocalDateTime.now()))
    val updatedPreviousStatuses = endCurrentStatus +: existingCourier.previousStatuses

    val newCourierStatus = CourierStatus(newStatusType)
    existingCourier.copy(currentStatus = newCourierStatus, previousStatuses = updatedPreviousStatuses)
  }
}


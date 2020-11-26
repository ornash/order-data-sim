package com.css.model

import com.css.exception.SimulatorException

import java.time.{Duration, LocalDateTime}

sealed trait CourierStatusType
case object DISPATCHED extends CourierStatusType
case object ARRIVED extends CourierStatusType
case object MATCHED extends CourierStatusType
case object HAS_DELIVERED extends CourierStatusType

case class CourierStatus(statusType: CourierStatusType,
                         startTime: LocalDateTime = LocalDateTime.now(),
                         endTime: Option[LocalDateTime] = None) {

  def durationInStatus: Duration = {
    if(endTime.isEmpty) {
      throw SimulatorException(s"End time is unavailable, cannot calculate time spent in state: $statusType")
    }

    Duration.between(startTime, endTime.get)
  }
}

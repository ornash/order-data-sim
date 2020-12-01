package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

sealed trait CourierStatusType
case object DISPATCHED extends CourierStatusType
case object ARRIVED extends CourierStatusType
case object MATCHED extends CourierStatusType
case object HAS_DELIVERED extends CourierStatusType

case class CourierStatus(statusType: CourierStatusType,
                         startTime: LocalDateTime = LocalDateTime.now(),
                         endTime: Option[LocalDateTime] = None) {

  def durationInStatus: Option[Duration] = {
    if(endTime.isDefined) {
      Some(Duration.between(startTime, endTime.get))
    } else {
      None
    }
  }

  override def toString: String = {
    s"CourierStatus($statusType at $startTime and took $durationInStatus)"
  }
}

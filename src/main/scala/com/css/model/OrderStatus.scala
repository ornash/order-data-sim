package com.css.model

import com.css.exception.SimulatorException

import java.time.{Duration, LocalDateTime}

sealed trait OrderStatusType
case object RECEIVED extends OrderStatusType
case object COOKING extends OrderStatusType
case object READY extends OrderStatusType
case object PICKED_UP extends OrderStatusType
case object DELIVERED extends OrderStatusType

case class OrderStatus(statusType: OrderStatusType,
                       startTime: LocalDateTime = LocalDateTime.now(),
                       endTime: Option[LocalDateTime] = None) {

  def durationInStatus: Duration = {
    if(endTime.isEmpty) {
      throw SimulatorException(s"End time is unavailable, cannot calculate time spent in state: $statusType")
    }

    Duration.between(startTime, endTime.get)
  }
}

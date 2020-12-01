package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

sealed trait OrderStatusType
case object RECEIVED extends OrderStatusType
case object COOKING extends OrderStatusType
case object READY extends OrderStatusType
case object PICKED_UP extends OrderStatusType
case object DELIVERED extends OrderStatusType

case class OrderStatus(statusType: OrderStatusType,
                       startTime: LocalDateTime = LocalDateTime.now(),
                       endTime: Option[LocalDateTime] = Option.empty) {

  def durationInStatus: Option[Duration] = {
    if(endTime.isDefined) {
      Some(Duration.between(startTime, endTime.get))
    } else {
      None
    }
  }

  override def toString: String = {
    s"OrderStatus($statusType at $startTime and took $durationInStatus)"
  }
}

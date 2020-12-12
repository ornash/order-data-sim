package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.Try

sealed trait OrderStatusType
case object RECEIVED extends OrderStatusType
case object COOKING extends OrderStatusType
case object READY extends OrderStatusType
case object PICKED_UP extends OrderStatusType
case object DELIVERED extends OrderStatusType

case class OrderStatus(statusType: OrderStatusType,
                       startTime: LocalDateTime = LocalDateTime.now(),
                       endTime: Option[LocalDateTime] = Option.empty,
                       previousStatus: Option[OrderStatus] = Option.empty) {

  override def toString: String = {
    s"OrderStatus(in $statusType for duration=$durationInStatus from ${startTime.toLocalTime} after ${previousStatus.toString})"
  }

  def findOrderStatus(expectedStatusType: OrderStatusType): Option[OrderStatus] = {
    if(this.statusType == expectedStatusType) {
      Some(this)
    } else if (previousStatus.isEmpty) {
      None
    } else {
      previousStatus.get.findOrderStatus(expectedStatusType)
    }
  }

  /**
   * @return true of OrderStatus is in COOKING state.
   */
  def isCooking(): Boolean = statusType == COOKING

  /**
   * @return true if OrderStatus is or has been in READY state.
   */
  def isReady(): Boolean = findOrderStatus(READY).isDefined

  /**
   * @return true if OrderStatus is or has been in PICKED_UP state.
   */
  def isPickedUp(): Boolean = findOrderStatus(PICKED_UP).isDefined

  /**
   * @return true if OrderStatus is or has been in DELIVERED state.
   */
  def isDelivered(): Boolean = findOrderStatus(DELIVERED).isDefined

  /**
   * @return true of OrderStatus was in READY, PICKED_UP or DELIVERED state.
   */
  def isCooked(): Boolean = isReady() || isPickedUp() || isDelivered()

  def durationInStatus: Duration = {
    Duration.between(startTime, endTime.getOrElse(LocalDateTime.now()))
  }

  def transform(newStatusType: OrderStatusType): Try[OrderStatus] = Try {
    if(!isValidTransformation(newStatusType)) {
      throw SimulatorException(s"Cannot transform OrderStatus from $statusType to $newStatusType")
    }
    val endCurrentStatus = copy(endTime = Some(LocalDateTime.now()))
    OrderStatus(newStatusType, previousStatus = Some(endCurrentStatus))
  }

  private def isValidTransformation(newStatusType: OrderStatusType): Boolean = {
    (statusType, newStatusType) match {
      case (RECEIVED, COOKING) => true
      case (COOKING, READY) => true
      case (READY, PICKED_UP) => true
      case (PICKED_UP, DELIVERED) => true
      case _ => false
    }
  }
}

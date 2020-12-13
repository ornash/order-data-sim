package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.Try

sealed trait OrderStatusType

case object RECEIVED extends OrderStatusType //Freshly received Order
case object COOKING extends OrderStatusType //Cooking Order
case object READY extends OrderStatusType //Cooked Order ready for pickup
case object PICKED_UP extends OrderStatusType //Order picked up by courier for delivery after match
case object DELIVERED extends OrderStatusType //Order delivered to customer

/**
 * Represents current status of an order along with its previous state.
 * The previous state will have its own previous state and so on.
 *
 * @param statusType     current state
 * @param startTime      time of entry into current state
 * @param endTime        time of exit from current state - optional
 * @param previousStatus previous state before current state - optional
 */
case class OrderStatus(statusType: OrderStatusType,
                       startTime: LocalDateTime = LocalDateTime.now(),
                       endTime: Option[LocalDateTime] = Option.empty,
                       previousStatus: Option[OrderStatus] = Option.empty) {

  override def toString: String = {
    s"OrderStatus(in $statusType for duration=$durationInStatus from ${startTime.toLocalTime} after ${previousStatus.toString})"
  }

  /**
   * @return duration in current state from startTime. If endTime is unspecified, duration is unspecified too.
   */
  def durationInStatus: Option[Duration] = endTime match {
    case Some(endTimeVal) => Some(Duration.between(startTime, endTimeVal))
    case None => None
  }

  /**
   * Recursive method to check if this status is equal to expectedOrderStatus or find expectedOrderStatus in any of
   * previous states of this status.
   */
  def findOrderStatus(expectedOrderStatus: OrderStatusType): Option[OrderStatus] = {
    if (this.statusType == expectedOrderStatus) {
      Some(this)
    } else if (previousStatus.isEmpty) {
      None
    } else {
      previousStatus.get.findOrderStatus(expectedOrderStatus)
    }
  }

  /**
   * @return true if this status is in COOKING state.
   */
  def isCooking(): Boolean = statusType == COOKING

  /**
   * @return true if this status is or has been in READY state.
   */
  def isReady(): Boolean = findOrderStatus(READY).isDefined

  /**
   * @return true if this status is or has been in PICKED_UP state.
   */
  def isPickedUp(): Boolean = findOrderStatus(PICKED_UP).isDefined

  /**
   * @return true if this status is or has been in DELIVERED state.
   */
  def isDelivered(): Boolean = findOrderStatus(DELIVERED).isDefined

  /**
   * @return true of this status was in READY, PICKED_UP or DELIVERED state.
   */
  def isCooked(): Boolean = isReady() || isPickedUp() || isDelivered()

  /**
   * Try transformation of current status into new status if the transition is valid. The try fails for an invalid
   * state transition. If successful, returned new status will have current status as its previous status with
   * current system time as endTime of current status.
   */
  def transform(newStatusType: OrderStatusType): Try[OrderStatus] = Try {
    if (!isValidTransformation(newStatusType)) {
      throw SimulatorException(s"Cannot transform OrderStatus from $statusType to $newStatusType")
    }
    val endCurrentStatus = copy(endTime = Some(LocalDateTime.now()))
    OrderStatus(newStatusType, previousStatus = Some(endCurrentStatus))
  }

  /**
   * @return true if transformation from this status to new status is valid.
   */
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

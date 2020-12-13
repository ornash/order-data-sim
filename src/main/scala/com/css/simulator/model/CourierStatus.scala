package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.Try

sealed trait CourierStatusType

case object DISPATCHED extends CourierStatusType //Courier dispatched after receiving order
case object ARRIVED extends CourierStatusType //Courier arrived at CloudKitchen
case object MATCHED extends CourierStatusType //Courier matched with and Order
case object HAS_DELIVERED extends CourierStatusType //Courier has delivered matched Order

/**
 * Represents current status of a courier along with its previous state.
 * The previous state will have its own previous state and so on.
 *
 * @param statusType     - current state
 * @param startTime      - time of entry into current state
 * @param endTime        - time of exit from current state - optional
 * @param previousStatus - previous state before current state - optional
 */
case class CourierStatus(statusType: CourierStatusType,
                         startTime: LocalDateTime = LocalDateTime.now(),
                         endTime: Option[LocalDateTime] = None,
                         previousStatus: Option[CourierStatus] = Option.empty) {

  override def toString: String = {
    s"CourierStatus(in $statusType for duration=$durationInStatus from ${startTime.toLocalTime} after ${previousStatus.toString})"
  }

  /**
   * @return duration in current state from startTime. If endTime is unspecified, duration is unspecified too.
   */
  def durationInStatus: Option[Duration] = endTime match {
    case Some(endTimeVal) => Some(Duration.between(startTime, endTimeVal))
    case None => None
  }

  /**
   * Recursive method to check if this status is equal to expectedCourierStatus or find expectedCourierStatus in any of
   * previous states of this status.
   */
  def findCourierStatus(expectedCourierStatus: CourierStatusType): Option[CourierStatus] = {
    if (this.statusType == expectedCourierStatus) {
      Some(this)
    } else if (previousStatus.isEmpty) {
      None
    } else {
      previousStatus.get.findCourierStatus(expectedCourierStatus)
    }
  }

  /**
   * @return true if this status is or has been in ARRIVED state.
   */
  def isArrived(): Boolean = findCourierStatus(ARRIVED).isDefined

  /**
   * @return true if this status is or has been in MATCHED state.
   */
  def isMatched(): Boolean = findCourierStatus(MATCHED).isDefined

  /**
   * @return true if this status is or has been in HAS_DELIVERED state.
   */
  def hasDelivered(): Boolean = findCourierStatus(HAS_DELIVERED).isDefined

  /**
   * Try transformation of current status into new status if the transition is valid. The try fails for an invalid
   * state transition. If successful, returned new status will have current status as its previous status with
   * current system time as endTime of current status.
   */
  def transform(newStatusType: CourierStatusType): Try[CourierStatus] = Try {
    if (!isValidTransformation(newStatusType)) {
      throw SimulatorException(s"Cannot transform CourierStatus from $statusType to $newStatusType")
    }

    val endCurrentStatus = copy(endTime = Some(LocalDateTime.now()))
    CourierStatus(newStatusType, previousStatus = Some(endCurrentStatus))
  }

  /**
   * @return true if transformation from this status to new status is valid.
   */
  private def isValidTransformation(newStatusType: CourierStatusType): Boolean = {
    (statusType, newStatusType) match {
      case (DISPATCHED, ARRIVED) => true
      case (ARRIVED, MATCHED) => true
      case (MATCHED, HAS_DELIVERED) => true
      case _ => false
    }
  }
}

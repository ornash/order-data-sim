package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

import scala.util.Try

sealed trait CourierStatusType
case object DISPATCHED extends CourierStatusType
case object ARRIVED extends CourierStatusType
case object MATCHED extends CourierStatusType
case object HAS_DELIVERED extends CourierStatusType

case class CourierStatus(statusType: CourierStatusType,
                         startTime: LocalDateTime = LocalDateTime.now(),
                         endTime: Option[LocalDateTime] = None,
                         previousStatus: Option[CourierStatus] = Option.empty) {

  override def toString: String = {
    s"CourierStatus(in $statusType for duration=$durationInStatus from ${startTime.toLocalTime} after ${previousStatus.toString})"
  }

  def findCourierStatus(expectedStatusType: CourierStatusType): Option[CourierStatus] = {
    if(this.statusType == expectedStatusType) {
      Some(this)
    } else if (previousStatus.isEmpty) {
      None
    } else {
      previousStatus.get.findCourierStatus(expectedStatusType)
    }
  }

  /**
   * @return true if CourierStatus is or has been in ARRIVED state.
   */
  def isArrived() : Boolean = findCourierStatus(ARRIVED).isDefined

  /**
   * @return true if CourierStatus is or has been in MATCHED state.
   */
  def isMatched() : Boolean = findCourierStatus(MATCHED).isDefined

  /**
   * @return true if CourierStatus is or has been in HAS_DELIVERED state.
   */
  def hasDelivered() : Boolean = findCourierStatus(HAS_DELIVERED).isDefined

  def durationInStatus: Duration = {
     Duration.between(startTime, endTime.getOrElse(LocalDateTime.now()))
  }

  def transform(newStatusType: CourierStatusType): Try[CourierStatus] = Try {
    if(!isValidTransformation(newStatusType)) {
      throw SimulatorException(s"Cannot transform CourierStatus from $statusType to $newStatusType")
    }
    val endCurrentStatus = copy(endTime = Some(LocalDateTime.now()))
    CourierStatus(newStatusType, previousStatus = Some(endCurrentStatus))
  }

  private def isValidTransformation(newStatusType: CourierStatusType): Boolean = {
    (statusType, newStatusType) match {
      case (DISPATCHED, ARRIVED) => true
      case (ARRIVED, MATCHED) => true
      case (MATCHED, HAS_DELIVERED) => true
      case _ => false
    }
  }
}

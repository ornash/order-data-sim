package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import org.scalatest.funsuite.AnyFunSuite

class CourierStatusTests extends AnyFunSuite {
  val START_TIME = LocalDateTime.now()
  val DURATION = Duration.ofSeconds(15)
  val END_TIME = START_TIME.plus(DURATION)
  val DISPATCHED_CS = CourierStatus(DISPATCHED, START_TIME, Some(END_TIME))

  test("Duration in status") {
    assertResult(DURATION)(DISPATCHED_CS.durationInStatus)
  }

  test("Duration in status without endTime") {
    val arrived = CourierStatus(ARRIVED, START_TIME, None)
    val firstDuration = arrived.durationInStatus
    assert(firstDuration.getNano > 0)
    //should be greater than previously returned duration
    assert(arrived.durationInStatus.compareTo(firstDuration) > 0)
  }

  test("Finding courier status same as current status") {
    val returnedStatus = DISPATCHED_CS.findCourierStatus(DISPATCHED)
    assert(returnedStatus.isDefined)
    assertResult(DISPATCHED_CS)(returnedStatus.get)
  }

  test("Finding courier status in one of previous states") {
    val arrived = DISPATCHED_CS.transform(ARRIVED).get
    assert(arrived.findCourierStatus(DISPATCHED).isDefined)
  }

  test("Finding courier status failure") {
    assert(DISPATCHED_CS.findCourierStatus(ARRIVED).isEmpty)
    val arrived = DISPATCHED_CS.transform(ARRIVED).get
    assert(arrived.findCourierStatus(MATCHED).isEmpty)
  }

  test("isArrived works") {
    assert(DISPATCHED_CS.isArrived() == false)
    assert(DISPATCHED_CS.transform(ARRIVED).get.isArrived())
  }

  test("isMatched works") {
    assert(DISPATCHED_CS.isMatched() == false)
    assert(DISPATCHED_CS.transform(ARRIVED).get.transform(MATCHED).get.isMatched())
  }

  test("hasDelivered works") {
    assert(DISPATCHED_CS.hasDelivered() == false)
    val arrived = DISPATCHED_CS.transform(ARRIVED).get
    assert(arrived.transform(MATCHED).get.transform(HAS_DELIVERED).get.hasDelivered())
  }

  test("valid transformations") {
    assert(DISPATCHED_CS.transform(ARRIVED).isSuccess)
    assert(DISPATCHED_CS.transform(ARRIVED).get.isArrived())
    val arrived = DISPATCHED_CS.transform(ARRIVED).get
    assert(arrived.transform(MATCHED).isSuccess)
    assert(arrived.transform(MATCHED).get.isMatched())
    assert(arrived.transform(MATCHED).get.transform(HAS_DELIVERED).isSuccess)
    assert(arrived.transform(MATCHED).get.transform(HAS_DELIVERED).get.hasDelivered())
  }

  test("invalid transformations") {
    assert(DISPATCHED_CS.transform(MATCHED).isFailure)
    assert(DISPATCHED_CS.transform(ARRIVED).get.transform(HAS_DELIVERED).isFailure)
  }
}
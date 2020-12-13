package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import org.scalatest.funsuite.AnyFunSuite

class OrderStatusTests extends AnyFunSuite {
  val START_TIME = LocalDateTime.now()
  val DURATION = Duration.ofSeconds(15)
  val END_TIME = START_TIME.plus(DURATION)
  val RECEIVED_OS = OrderStatus(RECEIVED, START_TIME, Some(END_TIME))

  test("Duration in status") {
    assertResult(DURATION)(RECEIVED_OS.durationInStatus)
  }

  test("Duration in status without endTime") {
    val cooking = OrderStatus(COOKING, START_TIME, None)
    assert(cooking.durationInStatus.isEmpty)
  }

  test("Finding order status same as current status") {
    val returnedStatus = RECEIVED_OS.findOrderStatus(RECEIVED)
    assert(returnedStatus.isDefined)
    assertResult(RECEIVED_OS)(returnedStatus.get)
  }

  test("Finding order status in one of previous states") {
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.findOrderStatus(RECEIVED).isDefined)
  }

  test("Finding order status failure") {
    assert(RECEIVED_OS.findOrderStatus(COOKING).isEmpty)
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.findOrderStatus(READY).isEmpty)
  }

  test("isCooking works") {
    assert(RECEIVED_OS.isCooking() == false)
    assert(RECEIVED_OS.transform(COOKING).get.isCooking())
  }

  test("isReady works") {
    assert(RECEIVED_OS.isReady() == false)
    assert(RECEIVED_OS.transform(COOKING).get.transform(READY).get.isReady())
  }

  test("isPickedup works") {
    assert(RECEIVED_OS.isPickedUp() == false)
    assert(RECEIVED_OS.transform(COOKING).get.transform(READY).get.transform(PICKED_UP).get.isPickedUp())
  }

  test("isDelivered works") {
    assert(RECEIVED_OS.isDelivered() == false)
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.transform(DELIVERED).get.isDelivered())
  }

  test("isCooked works") {
    assert(RECEIVED_OS.isCooked() == false)
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.transform(READY).get.isCooked())
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.isCooked())
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.transform(DELIVERED).get.isCooked())
  }

  test("valid transformations") {
    assert(RECEIVED_OS.transform(COOKING).isSuccess)
    assert(RECEIVED_OS.transform(COOKING).get.isCooking())
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.transform(READY).isSuccess)
    assert(cooking.transform(READY).get.isReady())
    assert(cooking.transform(READY).get.transform(PICKED_UP).isSuccess)
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.isPickedUp())
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.transform(DELIVERED).isSuccess)
    assert(cooking.transform(READY).get.transform(PICKED_UP).get.transform(DELIVERED).get.isDelivered())
  }

  test("invalid transformations") {
    assert(RECEIVED_OS.transform(READY).isFailure)
    val cooking = RECEIVED_OS.transform(COOKING).get
    assert(cooking.transform(PICKED_UP).isFailure)
    assert(cooking.transform(READY).get.transform(DELIVERED).isFailure)
  }
}
package com.css.simulator.model

import java.time.Duration

import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success}

class CourierTests extends AnyFunSuite {
  val ORDER_ID = "2123"
  val TRANSIT_DURATION = Duration.ofSeconds(10)
  val DISPATCHED_COURIER = Courier.dispatchNewCourier(Some(ORDER_ID), transitDuration = TRANSIT_DURATION)
  val SLEEP_DURATION = Duration.ofMillis(100)

  test("Construct new courier") {
    val newCourier = Courier.dispatchNewCourier(Some(ORDER_ID), transitDuration = TRANSIT_DURATION)
    assertResult(ORDER_ID)(newCourier.orderId.get)
    assertResult(TRANSIT_DURATION.getSeconds)(newCourier.transitDuration.getSeconds)
    assertResult(DISPATCHED)(newCourier.currentStatus.statusType)
  }

  test("Courier arrival successful") {
    val arrivedCourier = Courier.arrived(DISPATCHED_COURIER)
    assert(arrivedCourier.isSuccess)
    assert(arrivedCourier.get.currentStatus.isArrived())
  }

  test("Courier arrival failure") {
    val arrivedCourier = Courier.arrived(DISPATCHED_COURIER)
    assert(arrivedCourier.isSuccess)
    assert(Courier.arrived(arrivedCourier.get).isFailure)
  }

  test("Courier match successful") {
    val arrivedCourier = transformCourier(DISPATCHED_COURIER, ARRIVED)
    val matchedCourier = Courier.matched(arrivedCourier)
    assert(matchedCourier.isSuccess)
    assert(matchedCourier.get.currentStatus.isMatched())
  }

  test("Courier match failure") {
    assert(Courier.matched(DISPATCHED_COURIER).isFailure)
  }

  test("Courier delivery successful") {
    val matchedCourier = transformCourier(DISPATCHED_COURIER, MATCHED)
    val deliveredCourier = Courier.deliver(matchedCourier)
    assert(deliveredCourier.isSuccess)
    assert(deliveredCourier.get.currentStatus.hasDelivered())
  }

  test("Courier delivery failure") {
    assert(Courier.deliver(DISPATCHED_COURIER).isFailure)
  }

  test("Arrival Instant") {
    assert(DISPATCHED_COURIER.arrivalInstant().isEmpty)
    val arrivedCourier = transformCourier(DISPATCHED_COURIER, ARRIVED)
    assert(arrivedCourier.arrivalInstant().isDefined)
    assert(transformCourier(arrivedCourier, MATCHED).arrivalInstant().isDefined)
  }

  test("Duration unavailable") {
    assert(DISPATCHED_COURIER.dispatchDuration().isEmpty)
  }

  test("Dispatch duration") {
    val newCourier = Courier.dispatchNewCourier(Some(ORDER_ID), transitDuration = TRANSIT_DURATION)
    Thread.sleep(SLEEP_DURATION.toMillis)
    val arrivedCourier = transformCourier(newCourier, ARRIVED)
    assert(arrivedCourier.dispatchDuration().get.compareTo(SLEEP_DURATION) >= 0)
  }

  test("Wait duration") {
    val arrivedCourier = transformCourier(DISPATCHED_COURIER, ARRIVED)
    Thread.sleep(SLEEP_DURATION.toMillis)
    val matchedCourier = transformCourier(arrivedCourier, MATCHED)
    assert(matchedCourier.waitDuration().get.compareTo(SLEEP_DURATION) >= 0)
  }

  private def transformCourier(courier: Courier, to: CourierStatusType) : Courier = {
    if(courier.currentStatus.statusType.equals(to)){
      courier
    } else {
      courier.currentStatus.statusType match {
        case DISPATCHED => Courier.arrived(courier) match {
          case Failure(exception) => throw exception
          case Success(arrivedCourier) => transformCourier(arrivedCourier, to)
        }
        case ARRIVED => Courier.matched(courier) match {
          case Failure(exception) => throw exception
          case Success(matchedCourier) => transformCourier(matchedCourier, to)
        }
        case MATCHED => Courier.deliver(courier) match {
          case Failure(exception) => throw exception
          case Success(deliveredCourier) => transformCourier(deliveredCourier, to)
        }
        case HAS_DELIVERED => throw new Exception("invalid transformation")
      }
    }
  }
}

package com.css.simulator.strategy

import java.time.Duration

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class FifoMatchStrategyTests extends AnyFunSuite {
  val TRANSIT_DURATION = Duration.ofSeconds(10)
  val PREP_DURATION = Duration.ofSeconds(10)

  test("Fifo match strategy failure uncooked/not-ready order") {
    val fifoStrategy = FifoMatchStrategy()
    val newOrder = Order.newOrder(Random.nextInt().toString, prepDuration = PREP_DURATION)
    val courier1 = newArrivedCourier("1st") //arrived 1st

    var matches = fifoStrategy.matchReadyOrders(Seq(newOrder))
    assert(matches.isEmpty) //no courier has arrived

    assertThrows[SimulatorException] {
      matches = fifoStrategy.matchArrivedCouriers(Seq(courier1))
    }
  }

  test("Fifo match strategy failure dispatched courier") {
    val fifoStrategy = FifoMatchStrategy()
    val dispatchedCourier = Courier.dispatchNewCourier(transitDuration = TRANSIT_DURATION)

    var matches = fifoStrategy.matchReadyOrders(Seq(newReadyOrder()))
    assert(matches.isEmpty) //no courier has arrived

    assertThrows[SimulatorException] {
      matches = fifoStrategy.matchArrivedCouriers(Seq(dispatchedCourier))
    }
  }

  test("Fifo match strategy success") {
    val fifoStrategy = FifoMatchStrategy()
    val courier1 = newArrivedCourier("1st") //arrived 1st
    Thread.sleep(0, 1) //test with lowest precision
    val courier2 = newArrivedCourier("2nd") //arrived 2nd
    Thread.sleep(0, 1) //test with lowest precision
    val courier3 = newArrivedCourier("3rd") //arrived 3rd
    Thread.sleep(0, 1) //test with lowest precision
    val courier4 = newArrivedCourier("4th") //arrived 4th

    var matches = fifoStrategy.matchReadyOrders(Seq(newReadyOrder()))
    assert(matches.isEmpty) //no courier has arrived

    matches = fifoStrategy.matchArrivedCouriers(Seq(courier1))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(courier1.courierId)(matches(0).courier.courierId) //ready order should be matched with this courier

    matches = fifoStrategy.matchArrivedCouriers(Seq(courier4)) //insert courier4 first
    assert(matches.isEmpty) //no order is ready

    matches = fifoStrategy.matchArrivedCouriers(Seq(courier2)) //insert courier2 next
    assert(matches.isEmpty) //no order is ready

    matches = fifoStrategy.matchArrivedCouriers(Seq(courier3)) //insert courier3 last
    assert(matches.isEmpty) //no order is ready

    matches = fifoStrategy.matchReadyOrders(Seq(newReadyOrder()))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(courier2.courierId)(matches(0).courier.courierId) //courier2 should be matched

    matches = fifoStrategy.matchReadyOrders(Seq(newReadyOrder()))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(courier3.courierId)(matches(0).courier.courierId) //courier3 should be matched

    matches = fifoStrategy.matchReadyOrders(Seq(newReadyOrder()))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(courier4.courierId)(matches(0).courier.courierId) //courier4 should be matched
  }

  private def newReadyOrder(): Order = {
    val newOrder = Order.newOrder(Random.nextInt().toString, prepDuration = PREP_DURATION)
    val cookingOrder = Order.startCooking(newOrder)
    Order.readyForPickup(cookingOrder.get).get
  }

  private def newArrivedCourier(newCourierId: String): Courier = {
    Courier.arrived(Courier.dispatchNewCourier(courierId = newCourierId, transitDuration = TRANSIT_DURATION)).get
  }
}

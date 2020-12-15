package com.css.simulator.strategy

import java.time.Duration

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class OrderIdMatchStrategyTests extends AnyFunSuite {
  val TRANSIT_DURATION = Duration.ofSeconds(10)
  val PREP_DURATION = Duration.ofSeconds(10)

  test("OrderId match strategy failure when Courier doesnt have order-id") {
    val orderIdStrategy = OrderIdMatchStrategy()
    val newOrder = Order.newOrder(Random.nextInt().toString, prepDuration = PREP_DURATION)
    val courierWithoutOrder = newArrivedCourier("1st")

    var matches = orderIdStrategy.matchReadyOrders(Seq(newOrder))
    assert(matches.isEmpty) //no courier has arrived

    assertThrows[SimulatorException] {
      matches = orderIdStrategy.matchArrivedCouriers(Seq(courierWithoutOrder))
    }
  }

  test("OrderId match strategy failure for uncooked/not-ready order") {
    val orderIdStrategy = OrderIdMatchStrategy()
    val newOrder = Order.newOrder(Random.nextInt().toString, prepDuration = PREP_DURATION)
    val courier1 = newArrivedCourier(newOrder.id, "1st") //arrived 1st

    var matches = orderIdStrategy.matchReadyOrders(Seq(newOrder))
    assert(matches.isEmpty) //no courier has arrived

    assertThrows[SimulatorException] {
      matches = orderIdStrategy.matchArrivedCouriers(Seq(courier1))
    }
  }

  test("OrderId match strategy failure for dispatched courier") {
    val orderIdStrategy = OrderIdMatchStrategy()
    val readyOrder = newReadyOrder()
    val dispatchedCourier = Courier.dispatchNewCourier(Some(readyOrder.id), transitDuration = TRANSIT_DURATION)

    var matches = orderIdStrategy.matchReadyOrders(Seq(readyOrder))
    assert(matches.isEmpty) //no courier has arrived

    assertThrows[SimulatorException] {
      matches = orderIdStrategy.matchArrivedCouriers(Seq(dispatchedCourier))
    }
  }

  test("OrderId match strategy success") {
    val orderIdStrategy = OrderIdMatchStrategy()
    val order1 = newReadyOrder()
    val order2 = newReadyOrder()
    val order3 = newReadyOrder()
    val order4 = newReadyOrder()
    val courier1 = newArrivedCourier(order1.id, "1st") //arrived 1st
    val courier2 = newArrivedCourier(order2.id, "2nd") //arrived 2nd
    val courier3 = newArrivedCourier(order3.id, "3rd") //arrived 3rd
    val courier4 = newArrivedCourier(order4.id, "4th") //arrived 4th

    var matches = orderIdStrategy.matchReadyOrders(Seq(order1))
    assert(matches.isEmpty) //no courier has arrived

    matches = orderIdStrategy.matchArrivedCouriers(Seq(courier1))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(order1.id)(matches(0).courier.orderId.get) //order-id should match
    assertResult(order1.id)(matches(0).order.id) //order-id should match
    assertResult(courier1.courierId)(matches(0).courier.courierId) //ready order should be matched with this courier

    matches = orderIdStrategy.matchArrivedCouriers(Seq(courier4)) //insert courier4 first
    assert(matches.isEmpty) //no order is ready

    matches = orderIdStrategy.matchArrivedCouriers(Seq(courier2)) //insert courier2 next
    assert(matches.isEmpty) //no order is ready

    matches = orderIdStrategy.matchArrivedCouriers(Seq(courier3)) //insert courier3 last
    assert(matches.isEmpty) //no order is ready

    matches = orderIdStrategy.matchReadyOrders(Seq(order2))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(order2.id)(matches(0).courier.orderId.get) //order-id should match
    assertResult(order2.id)(matches(0).order.id) //order-id should match
    assertResult(courier2.courierId)(matches(0).courier.courierId) //courier2 should be matched

    matches = orderIdStrategy.matchReadyOrders(Seq(order3))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(order3.id)(matches(0).courier.orderId.get) //order-id should match
    assertResult(order3.id)(matches(0).order.id) //order-id should match
    assertResult(courier3.courierId)(matches(0).courier.courierId) //courier3 should be matched

    matches = orderIdStrategy.matchReadyOrders(Seq(order4))
    assert(matches.nonEmpty && matches.size == 1)
    assertResult(order4.id)(matches(0).courier.orderId.get) //order-id should match
    assertResult(order4.id)(matches(0).order.id) //order-id should match
    assertResult(courier4.courierId)(matches(0).courier.courierId) //courier4 should be matched
  }

  private def newReadyOrder(): Order = {
    val newOrder = Order.newOrder(Random.nextInt().toString, prepDuration = PREP_DURATION)
    val cookingOrder = Order.startCooking(newOrder)
    Order.readyForPickup(cookingOrder.get).get
  }

  private def newArrivedCourier(orderId: String, newCourierId: String): Courier = {
    Courier.arrived(Courier.dispatchNewCourier(Some(orderId),  newCourierId, TRANSIT_DURATION)).get
  }

  private def newArrivedCourier(newCourierId: String): Courier = {
    Courier.arrived(Courier.dispatchNewCourier(None,  newCourierId, TRANSIT_DURATION)).get
  }
}

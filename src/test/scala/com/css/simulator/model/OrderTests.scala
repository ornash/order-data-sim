package com.css.simulator.model

import java.time.Duration

import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success}

class OrderTests extends AnyFunSuite {
  val ID = "2123"
  val NAME = "cake"
  val PREP_TIME_SECONDS = 12
  val ORDER_NOTIFICATION = OrderNotification(ID, NAME, PREP_TIME_SECONDS)
  val NEW_ORDER = Order.newOrder(ID, NAME, PREP_TIME_SECONDS)
  val SLEEP_DURATION = Duration.ofMillis(100)

  test("Construct order from notification") {
    val newOrder = Order.fromOrderNotification(ORDER_NOTIFICATION)
    assertResult(ORDER_NOTIFICATION.id)(newOrder.id)
    assertResult(ORDER_NOTIFICATION.name)(newOrder.name)
    assertResult(ORDER_NOTIFICATION.prepTime)(newOrder.prepDuration.getSeconds)
    assertResult(RECEIVED)(newOrder.currentStatus.statusType)
  }

  test("Construct new order") {
    val newOrder = Order.newOrder(ID, NAME, PREP_TIME_SECONDS)
    assertResult(ID)(newOrder.id)
    assertResult(NAME)(newOrder.name)
    assertResult(PREP_TIME_SECONDS)(newOrder.prepDuration.getSeconds)
    assertResult(RECEIVED)(newOrder.currentStatus.statusType)
  }

  test("Start cooking order successful") {
    val cookingOrder = Order.startCooking(NEW_ORDER)
    assert(cookingOrder.isSuccess)
    assert(cookingOrder.get.currentStatus.isCooking())
  }

  test("Start cooking order failure") {
    val cookingOrder = Order.startCooking(NEW_ORDER)
    assert(cookingOrder.isSuccess)
    assert(Order.startCooking(cookingOrder.get).isFailure)
  }

  test("Order ready successful") {
    val cookingOrder = transformOrder(NEW_ORDER, COOKING)
    val readyOrder = Order.readyForPickup(cookingOrder)
    assert(readyOrder.isSuccess)
    assert(readyOrder.get.currentStatus.isReady())
  }

  test("Order ready failure") {
    assert(Order.readyForPickup(NEW_ORDER).isFailure)
  }

  test("Order pickup successful") {
    val readyOrder = transformOrder(NEW_ORDER, READY)
    val pickedupOrder = Order.pickup(readyOrder)
    assert(pickedupOrder.isSuccess)
    assert(pickedupOrder.get.currentStatus.isPickedUp())
  }

  test("Order pickup failure") {
    assert(Order.pickup(NEW_ORDER).isFailure)
  }

  test("Order delivery successful") {
    val pickedOrder = transformOrder(NEW_ORDER, PICKED_UP)
    val deliveredOrder = Order.deliver(pickedOrder)
    assert(deliveredOrder.isSuccess)
    assert(deliveredOrder.get.currentStatus.isDelivered())
  }

  test("Order delivery failure") {
    assert(Order.deliver(NEW_ORDER).isFailure)
  }

  test("Duration unavailable") {
    assert(NEW_ORDER.schedulerDelayDuration().isEmpty)
  }

  test("Scheduler delay duration") {
    val newOrder = Order.newOrder(ID, NAME, PREP_TIME_SECONDS)
    Thread.sleep(SLEEP_DURATION.toMillis)
    val cookingOrder = transformOrder(newOrder, COOKING)
    assert(cookingOrder.schedulerDelayDuration().get.compareTo(SLEEP_DURATION) >= 0)
  }

  test("Cook duration") {
    val cookingOrder = transformOrder(NEW_ORDER, COOKING)
    Thread.sleep(SLEEP_DURATION.toMillis)
    val readyOrder = transformOrder(cookingOrder, READY)
    assert(readyOrder.cookDuration().get.compareTo(SLEEP_DURATION) >= 0)
  }

  test("Wait duration") {
    val readyOrder = transformOrder(NEW_ORDER, READY)
    Thread.sleep(SLEEP_DURATION.toMillis)
    val pickedupOrder = transformOrder(readyOrder, PICKED_UP)
    assert(pickedupOrder.waitDuration().get.compareTo(SLEEP_DURATION) >= 0)
  }

  private def transformOrder(order: Order, to: OrderStatusType) : Order = {
    if(order.currentStatus.statusType.equals(to)){
      order
    } else {
      order.currentStatus.statusType match {
        case RECEIVED => Order.startCooking(order) match {
          case Failure(exception) => throw exception
          case Success(cookingOrder) => transformOrder(cookingOrder, to)
        }
        case COOKING => Order.readyForPickup(order) match {
          case Failure(exception) => throw exception
          case Success(readyOrder) => transformOrder(readyOrder, to)
        }
        case READY => Order.pickup(order) match {
          case Failure(exception) => throw exception
          case Success(pickedOrder) => transformOrder(pickedOrder, to)
        }
        case PICKED_UP => Order.deliver(order) match {
          case Failure(exception) => throw exception
          case Success(deliverOrder) => transformOrder(deliverOrder, to)
        }
        case DELIVERED => throw new Exception("invalid transformation")
      }
    }
  }
}

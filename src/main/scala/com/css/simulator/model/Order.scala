package com.css.simulator.model

import java.time.Duration

import com.css.simulator.exception.SimulatorException

import scala.util.{Failure, Success, Try}

case class Order(id: String,
                 name: String = "",
                 prepDuration: Duration,
                 currentStatus: OrderStatus) {

  def transform(newStatusType: OrderStatusType): Try[Order] = Try {
    currentStatus.transform(newStatusType) match {
      case Success(newStatus) => this.copy(currentStatus = newStatus)
      case Failure(exception) => throw SimulatorException(s"Cannot transform Order to $newStatusType", exception)
    }
  }

  def receivedDuration(): Option[Duration] = {
    durationInStatus(RECEIVED)
  }

  def cookDuration(): Option[Duration] = {
    durationInStatus(COOKING)
  }

  def waitDuration(): Option[Duration] = {
    durationInStatus(READY)
  }

  private def durationInStatus(expectedStatusType: OrderStatusType): Option[Duration] = {
    currentStatus.findOrderStatus(expectedStatusType) match {
      case Some(expectedOrderStatus) => Some(expectedOrderStatus.durationInStatus)
      case None => None
    }
  }
}

object Order {
  val DUMMY_ORDER = newOrder("dummy", "dummy", 0)

  def newOrder(id: String, name: String = "", prepTimeInSeconds: Int): Order = {
    Order(id, name, Duration.ofSeconds(prepTimeInSeconds), OrderStatus(RECEIVED))
  }

  def fromOrderNotification(orderNotification: OrderNotification): Order = {
    newOrder(orderNotification.id, orderNotification.name, orderNotification.prepTime)
  }

  def startCooking(receivedOrder: Order): Try[Order] = {
    receivedOrder.transform(COOKING)
  }

  def readyForPickup(cookedOrder: Order): Try[Order] = {
    cookedOrder.transform(READY)
  }

  def pickup(readyOrder: Order): Try[Order] = {
    readyOrder.transform(PICKED_UP)
  }

  def deliver(pickedOrder: Order): Try[Order] = {
    pickedOrder.transform(DELIVERED)
  }
}
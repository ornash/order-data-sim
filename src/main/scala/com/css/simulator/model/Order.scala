package com.css.simulator.model

import java.time.Duration

import com.css.simulator.exception.SimulatorException

import scala.util.{Failure, Success, Try}

/**
 * Represents an Order in its current status.
 *
 * @param id            order id
 * @param name
 * @param prepDuration  duration required to cook order
 * @param currentStatus current status of order.
 */
case class Order(id: String,
                 name: String = "",
                 prepDuration: Duration,
                 currentStatus: OrderStatus) {

  /**
   * Tries to transform this Order from current state to a new state.
   */
  def transform(newStatusType: OrderStatusType): Try[Order] = Try {
    currentStatus.transform(newStatusType) match {
      case Success(newStatus) => this.copy(currentStatus = newStatus)
      case Failure(exception) => throw SimulatorException(s"Cannot transform Order to $newStatusType", exception)
    }
  }

  /**
   * @return duration spent in received state before starting to cook the order
   */
  def schedulerDelayDuration(): Option[Duration] = {
    durationInStatus(RECEIVED)
  }

  /**
   * @return duration spent in cooking the order. Should be greater than or equal to prepDuration.
   */
  def cookDuration(): Option[Duration] = {
    durationInStatus(COOKING)
  }

  /**
   * @return duration spent waiting after an order is cooked/ready until its match with a [[com.css.simulator.model.Courier]]
   */
  def waitDuration(): Option[Duration] = {
    durationInStatus(READY)
  }

  private def durationInStatus(expectedStatusType: OrderStatusType): Option[Duration] = {
    currentStatus.findOrderStatus(expectedStatusType) match {
      case Some(expectedOrderStatus) => expectedOrderStatus.durationInStatus
      case None => None
    }
  }
}

object Order {
  private val dummy = "dummy"
  val DUMMY_ORDER = newOrder(dummy,dummy, 0)

  def fromOrderNotification(orderNotification: OrderNotification): Order = {
    newOrder(orderNotification.id, orderNotification.name, orderNotification.prepTime)
  }

  def newOrder(id: String, name: String = "", prepTimeInSeconds: Int): Order = {
    Order(id, name, Duration.ofSeconds(prepTimeInSeconds), OrderStatus(RECEIVED))
  }

  /**
   * Starts cooking a received order.
   */
  def startCooking(receivedOrder: Order): Try[Order] = {
    receivedOrder.transform(COOKING)
  }

  /**
   * Makes a cooking order ready/cooked for pickup.
   */
  def readyForPickup(cookingOrder: Order): Try[Order] = {
    cookingOrder.transform(READY)
  }

  /**
   * Picks up a ready/cooked order for delivery.
   */
  def pickup(readyOrder: Order): Try[Order] = {
    readyOrder.transform(PICKED_UP)
  }

  /**
   * Delivers picked up order.
   */
  def deliver(pickedOrder: Order): Try[Order] = {
    pickedOrder.transform(DELIVERED)
  }
}
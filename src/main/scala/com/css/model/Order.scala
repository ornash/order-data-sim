package com.css.model

import java.time.{Duration, LocalDateTime}

import com.css.exception.SimulatorException

case class Order(id: String,
                 name: String = "",
                 prepDuration: Duration,
                 currentStatus: OrderStatus,
                 previousStatuses: Seq[OrderStatus] = Seq.empty) {

  def isReady(): Boolean = {
    currentStatus.statusType == READY
  }

  def isPickedUp(): Boolean = {
    currentStatus.statusType == PICKED_UP
  }

  def isDelivered(): Boolean = {
    currentStatus.statusType == DELIVERED
  }

  def waitDuration(): Duration = {
    val currentStatusType = currentStatus.statusType
    if(currentStatusType != PICKED_UP && currentStatusType != DELIVERED) {
      throw SimulatorException(s"Cannot calculate wait duration of order $this, it isn't picked up or delivered.")
    }

    val readyStatus = previousStatuses.find(READY == _.statusType)
    if(readyStatus.isEmpty) {
      throw SimulatorException(s"Cannot calculate wait duration of order $this, it didn't enter ready state.")
    }

    readyStatus.get.durationInStatus
  }
}

object Order {
  //FIXME
  val LAST_ORDER = Order.newOrder("last", "last", 0)

  def newOrder(id: String,
               name: String = "",
               prepTime: Int): Order = {
    Order(id, name, Duration.ofSeconds(prepTime), OrderStatus(RECEIVED))
  }

  def fromOrderNotification(orderNotification: OrderNotification): Order = {
    Order(orderNotification.id, orderNotification.name, Duration.ofSeconds(orderNotification.prepTime), OrderStatus(RECEIVED))
  }

  def startCooking(receivedOrder: Order): Order = {
    changeOrderStatus(receivedOrder, RECEIVED, COOKING)
  }

  def readyForPickup(cookedOrder: Order): Order = {
    changeOrderStatus(cookedOrder, COOKING, READY)
  }

  def pickup(readyOrder: Order): Order = {
    changeOrderStatus(readyOrder, READY, PICKED_UP)
  }

  def deliver(pickedOrder: Order): Order = {
    changeOrderStatus(pickedOrder, PICKED_UP, DELIVERED)
  }

  private def changeOrderStatus(existingOrder: Order,
                                expectedCurrentStatusType: OrderStatusType,
                                newStatusType: OrderStatusType): Order = {
    val currentStatus = existingOrder.currentStatus
    if(currentStatus.statusType != expectedCurrentStatusType) {
      throw SimulatorException(s"Cannot change order status for $existingOrder, order is not : $expectedCurrentStatusType")
    }

    val endCurrentStatus = currentStatus.copy(endTime = Some(LocalDateTime.now()))
    val updatedPreviousStatuses = endCurrentStatus +: existingOrder.previousStatuses

    val newOrderStatus = OrderStatus(newStatusType)
    existingOrder.copy(currentStatus = newOrderStatus, previousStatuses = updatedPreviousStatuses)
  }
}
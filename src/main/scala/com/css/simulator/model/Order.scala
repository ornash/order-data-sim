package com.css.simulator.model

import java.time.{Duration, LocalDateTime}

import com.css.simulator.exception.SimulatorException

//FIXME - allow only linear transitions, no cyclic transitions
//FIXME - Transitions from one state to another should happen instantaneously within this class, dont allow delays. -handled
//FIXME - dont throw exceptions, use Try/Success/Failure or Option
case class Order(id: String,
                 name: String = "",
                 prepDuration: Duration,
                 currentStatus: OrderStatus,
                 previousStatuses: Seq[OrderStatus] = Seq.empty) {

  def isCooking(): Boolean = {
    currentStatus.statusType == COOKING
  }

  def isReady(): Boolean = {
    currentStatus.statusType == READY
  }

  def isPickedUp(): Boolean = {
    currentStatus.statusType == PICKED_UP
  }

  def isDelivered(): Boolean = {
    currentStatus.statusType == DELIVERED
  }

  def isCooked(): Boolean = {
    isReady() || isPickedUp() || isDelivered()
  }

  def receivedDuration(): Duration = {
    if(isCooking() || isCooked()) {
      val receivedStatus = previousStatuses.find(RECEIVED == _.statusType)
      if(receivedStatus.isEmpty) {
        throw SimulatorException(s"Cannot calculate received duration of order $this, it didn't enter received state.")
      }

      receivedStatus.get.durationInStatus.get
    } else {
      throw SimulatorException(s"Cannot calculate received duration of order $this, it isn't received yet.")
    }
  }

  def cookDuration(): Duration = {
    if(isCooked()) {
      val cookingStatus = previousStatuses.find(COOKING == _.statusType)
      if(cookingStatus.isEmpty) {
        throw SimulatorException(s"Cannot calculate cook duration of order $this, it didn't enter cooking state.")
      }

      cookingStatus.get.durationInStatus.get
    } else {
      throw SimulatorException(s"Cannot calculate cook duration of order $this, it isn't cooked yet.")
    }
  }

  def waitDuration(): Duration = {
    if(isPickedUp() || isDelivered()) {
      val readyStatus = previousStatuses.find(READY == _.statusType)
      if (readyStatus.isEmpty) {
        throw SimulatorException(s"Cannot calculate wait duration of order $this, it didn't enter ready state.")
      }

      readyStatus.get.durationInStatus.get
    } else {
      throw SimulatorException(s"Cannot calculate wait duration of order $this, it isn't picked up or delivered.")
    }
  }
}

object Order {
  //FIXME
  val LAST_ORDER = Order.newOrder("last", "last", 0)

  def newOrder(id: String, name: String = "", prepTime: Int): Order = {
    Order(id, name, Duration.ofSeconds(prepTime), OrderStatus(RECEIVED))
  }

  def fromOrderNotification(orderNotification: OrderNotification): Order = {
    newOrder(orderNotification.id, orderNotification.name, orderNotification.prepTime)
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
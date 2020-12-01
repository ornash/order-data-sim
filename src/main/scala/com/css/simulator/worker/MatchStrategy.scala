package com.css.simulator.worker

import com.css.simulator.model.{Courier, Order}

import scala.collection.mutable

trait MatchStrategy {
  protected val matchedOrders = mutable.ArrayBuffer.empty[Order]
  protected val matchedCouriers = mutable.ArrayBuffer.empty[Courier]

  //FIXME: can you do it using single order or courier instead? i.e. using processOrder() and processCourier()
  def processBatch(ordersBatch: Seq[Order], couriersBatch: Seq[Courier])

  def getMatchedOrders(): Seq[Order] = {
    matchedOrders.toSeq
  }
  def getMatchedCouriers(): Seq[Courier] = {
    matchedCouriers.toSeq
  }
}

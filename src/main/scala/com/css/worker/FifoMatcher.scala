package com.css.worker

import java.time.LocalDateTime
import java.util
import java.util.concurrent.LinkedBlockingDeque

import com.css.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class FifoMatcher(ec: ExecutionContext,
                       orderQueue: LinkedBlockingDeque[Order],
                       courierQueue: LinkedBlockingDeque[Courier]) extends LazyLogging {

  implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  val matchedOrders = mutable.ArrayBuffer.empty[Order]
  val matchedCouriers = mutable.ArrayBuffer.empty[Courier]

  val cookedOrders = mutable.ArrayBuffer.empty[Order]
  val arrivedCouriers = mutable.PriorityQueue()(Ordering.by[Courier, LocalDateTime](_.arrivalTime())(dateTimeOrdering))

  def processCompletedOrders(): Unit = {
    var lastOrderCooked = false
    var lastCourierArrived = false
    while(lastCourierArrived == false || lastOrderCooked == false) {
      val cookedOrdersList = new util.ArrayList[Order]()
      orderQueue.drainTo(cookedOrdersList)
      cookedOrdersList.asScala.foreach(cookedOrder => {
        if(cookedOrder == Order.LAST_ORDER) {
          lastOrderCooked = true
        } else {
          cookedOrders.addOne(cookedOrder)
        }
      })

      val arrivedCouriersList = new util.ArrayList[Courier]()
      courierQueue.drainTo(arrivedCouriersList)
      arrivedCouriersList.asScala.foreach(arrivedCourier => {
        if(Courier.LAST_COURIER == arrivedCourier) {
          lastCourierArrived = true
        } else {
          arrivedCouriers.addOne(arrivedCourier)
        }
      })

      val cookedOrdersToBeRemoved = mutable.ArrayBuffer.empty[Order]

      cookedOrders.foreach(cookedOrder => {
        Try(arrivedCouriers.dequeue()) match {
          case Success(arrivedCourier) => {
            val matchedCourier = Courier.matched(arrivedCourier)
            //FIXME which order did this match with
            matchedCouriers.addOne(matchedCourier)

            val matchedOrder = Order.pickup(cookedOrder)
            cookedOrdersToBeRemoved.addOne(cookedOrder)
            matchedOrders.addOne(matchedOrder)
          }
          case Failure(exception) => {}
        }
      })

      cookedOrders.subtractAll(cookedOrdersToBeRemoved)
      Thread.sleep(1000)
    }
  }
}

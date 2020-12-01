package com.css.simulator.worker

import java.time.LocalDateTime
import java.util
import java.util.concurrent.LinkedBlockingDeque

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

case class Matcher(ec: ExecutionContext,
                   orderQueue: LinkedBlockingDeque[Order],
                   courierQueue: LinkedBlockingDeque[Courier],
                   matchStrategy: MatchStrategy) extends LazyLogging {

  implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  def processCompletedOrders(): Unit = {
    var lastOrderCooked = false
    var lastCourierArrived = false
    while(lastCourierArrived == false || lastOrderCooked == false) {
      val cookedOrdersList = new util.ArrayList[Order]()
      orderQueue.drainTo(cookedOrdersList)
      lastOrderCooked = cookedOrdersList.asScala.find(cookedOrder => cookedOrder == Order.LAST_ORDER).isDefined
      val cookedOrdersBatch = cookedOrdersList.asScala.filterNot(cookedOrder => cookedOrder == Order.LAST_ORDER).toSeq

      val arrivedCouriersList = new util.ArrayList[Courier]()
      courierQueue.drainTo(arrivedCouriersList)
      lastCourierArrived = arrivedCouriersList.asScala.find(arrivedCourier => arrivedCourier == Courier.LAST_COURIER).isDefined
      val arrivedCouriersBatch = arrivedCouriersList.asScala.filterNot(arrivedCourier => arrivedCourier == Courier.LAST_COURIER).toSeq

      matchStrategy.processBatch(cookedOrdersBatch, arrivedCouriersBatch)
      Thread.sleep(500)
    }
  }
}

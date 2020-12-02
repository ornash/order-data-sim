package com.css.simulator.worker

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

  def processCompletedOrders(): Unit = {
    var isLastOrderCooked = false
    var hasLastCourierArrived = false
    while(hasLastCourierArrived == false || isLastOrderCooked == false) {
      val cookedOrdersList = new util.ArrayList[Order]()
      orderQueue.drainTo(cookedOrdersList)
      isLastOrderCooked = cookedOrdersList.asScala.find(cookedOrder => cookedOrder == Order.DUMMY_ORDER).isDefined
      val cookedOrdersBatch = cookedOrdersList.asScala.filterNot(cookedOrder => cookedOrder == Order.DUMMY_ORDER).toSeq

      val arrivedCouriersList = new util.ArrayList[Courier]()
      courierQueue.drainTo(arrivedCouriersList)
      hasLastCourierArrived = arrivedCouriersList.asScala.find(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).isDefined
      val arrivedCouriersBatch = arrivedCouriersList.asScala.filterNot(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).toSeq

      matchStrategy.processBatch(cookedOrdersBatch, arrivedCouriersBatch)
      Thread.sleep(500)
    }
  }
}

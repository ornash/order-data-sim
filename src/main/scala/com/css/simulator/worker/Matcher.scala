package com.css.simulator.worker

import java.util
import java.util.concurrent.LinkedBlockingQueue

import com.css.simulator.model.{Courier, Order}
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

case class Matcher(ec: ExecutionContext,
                   orderQueue: LinkedBlockingQueue[Order],
                   courierQueue: LinkedBlockingQueue[Courier],
                   matchStrategy: MatchStrategy) extends LazyLogging {

  def processCompletedOrders(): Unit = {
    var isLastOrderCooked = false
    var hasLastCourierArrived = false
    while(hasLastCourierArrived == false || isLastOrderCooked == false) {
      if(isLastOrderCooked == false) {
        val cookedOrdersList = new util.ArrayList[Order]()
        //blocking operation
        cookedOrdersList.add(orderQueue.take())
        orderQueue.drainTo(cookedOrdersList)
        isLastOrderCooked = cookedOrdersList.asScala.find(cookedOrder => cookedOrder == Order.DUMMY_ORDER).isDefined
        val cookedOrdersBatch = cookedOrdersList.asScala.filterNot(cookedOrder => cookedOrder == Order.DUMMY_ORDER).toSeq
        matchStrategy.processBatch(cookedOrdersBatch, Seq.empty[Courier])
      }

      if(hasLastCourierArrived == false) {
        val arrivedCouriersList = new util.ArrayList[Courier]()
        //blocking operation
        arrivedCouriersList.add(courierQueue.take())
        courierQueue.drainTo(arrivedCouriersList)
        hasLastCourierArrived = arrivedCouriersList.asScala.find(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).isDefined
        val arrivedCouriersBatch = arrivedCouriersList.asScala.filterNot(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).toSeq

        matchStrategy.processBatch(Seq.empty[Order], arrivedCouriersBatch)
      }
      //Thread.sleep(500)
    }
  }
}

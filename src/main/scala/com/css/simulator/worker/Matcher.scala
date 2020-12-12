package com.css.simulator.worker

import java.util
import java.util.concurrent.{Executors, LinkedBlockingQueue}

import com.css.simulator.model.{Courier, Order}
import com.css.simulator.strategy.MatchStrategy
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

case class Matcher(orderQueue: LinkedBlockingQueue[Order],
                   courierQueue: LinkedBlockingQueue[Courier],
                   matchStrategy: MatchStrategy) extends LazyLogging {

  implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  def startProcessingCookedOrders(): Future[Boolean] = Future {
    var isLastOrderCooked = false
    var hasLastCourierArrived = false
    while(hasLastCourierArrived == false || isLastOrderCooked == false) {
      if(hasLastCourierArrived == false) {
        val arrivedCouriersList = new util.ArrayList[Courier]()
        //blocking operation
        arrivedCouriersList.add(courierQueue.take())
        courierQueue.drainTo(arrivedCouriersList)
        hasLastCourierArrived = arrivedCouriersList.asScala.find(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).isDefined
        val arrivedCouriersBatch = arrivedCouriersList.asScala.filterNot(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).toSeq

        matchStrategy.matchArrivedCouriers(arrivedCouriersBatch)
      }

      if(isLastOrderCooked == false) {
        val cookedOrdersList = new util.ArrayList[Order]()
        //blocking operation
        cookedOrdersList.add(orderQueue.take())
        orderQueue.drainTo(cookedOrdersList)
        isLastOrderCooked = cookedOrdersList.asScala.find(cookedOrder => cookedOrder == Order.DUMMY_ORDER).isDefined
        val cookedOrdersBatch = cookedOrdersList.asScala.filterNot(cookedOrder => cookedOrder == Order.DUMMY_ORDER).toSeq
        matchStrategy.matchCookedOrders(cookedOrdersBatch)
      }
    }

    isLastOrderCooked && hasLastCourierArrived
  }
}

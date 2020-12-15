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

  def startProcessingReadyOrders(): Future[Boolean] = Future {
    var isLastOrderReady = false
    var hasLastCourierArrived = false
    while(hasLastCourierArrived == false || isLastOrderReady == false) {
      if(hasLastCourierArrived == false) {
        val arrivedCouriersList = new util.ArrayList[Courier]()
        //blocking operation
        arrivedCouriersList.add(courierQueue.take())
        courierQueue.drainTo(arrivedCouriersList)
        hasLastCourierArrived = arrivedCouriersList.asScala.find(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).isDefined
        val arrivedCouriersBatch = arrivedCouriersList.asScala.filterNot(arrivedCourier => arrivedCourier == Courier.DUMMY_COURIER).toSeq

        matchStrategy.matchArrivedCouriers(arrivedCouriersBatch)
      }

      if(isLastOrderReady == false) {
        val readyOrdersList = new util.ArrayList[Order]()
        //blocking operation
        readyOrdersList.add(orderQueue.take())
        orderQueue.drainTo(readyOrdersList)
        isLastOrderReady = readyOrdersList.asScala.find(readyOrder => readyOrder == Order.DUMMY_ORDER).isDefined
        val readyOrdersBatch = readyOrdersList.asScala.filterNot(readyOrder => readyOrder == Order.DUMMY_ORDER).toSeq
        matchStrategy.matchReadyOrders(readyOrdersBatch)
      }
    }

    isLastOrderReady && hasLastCourierArrived
  }
}

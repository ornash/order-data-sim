package com.css.simulator.worker

import java.util
import java.util.concurrent.{Executors, LinkedBlockingQueue}

import com.css.simulator.model.{Courier, Order}
import com.css.simulator.strategy.MatchStrategy
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/**
 * Matches orders from orderQueue with couriers from courierQueue based on specified matchStrategy.
 */
case class Matcher(orderQueue: LinkedBlockingQueue[Order],
                   courierQueue: LinkedBlockingQueue[Courier],
                   matchStrategy: MatchStrategy) extends LazyLogging {

  implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  /**
   * Starts matching process between ready orders and arrived couriers.
   * This process stops when both last order is ready and last courier has arrived.
   * Expected last order object is Order.DUMMY_ORDER.
   * Expected last courier object is Courier.DUMMY_COURIER.
   *
   * @return an awaitable future
   */
  def startMatchProcessing(): Future[Boolean] = Future {
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

package com.css.simulator.worker

import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue

import com.css.simulator.SimulatorConfig
import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, Order, OrderNotification}
import com.css.simulator.strategy.{FifoMatchStrategy, OrderIdMatchStrategy}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success, Try}

case class CloudKitchen(simulatorConfig: SimulatorConfig, orderQueue: LinkedBlockingQueue[Order], courierQueue: LinkedBlockingQueue[Courier]) extends LazyLogging {

  private val courierDispatchScheduler = DelayedTaskScheduler(simulatorConfig.totalWorkerThreads)
  private val orderScheduler = DelayedTaskScheduler(simulatorConfig.totalWorkerThreads)

  def cookOrder(orderNotification: OrderNotification): Future[Try[Order]] = {
    val receivedOrder = Order.fromOrderNotification(orderNotification)

    val cookingOrder = Order.startCooking(receivedOrder)
    val chefsPromise = if (cookingOrder.isFailure) {
      Promise[Try[Order]]().failure(SimulatorException(s"Failed to start cooking order: $receivedOrder", cookingOrder.failed.get))
    } else {
      val cookingDurationInSeconds = cookingOrder.get.prepDuration.getSeconds
      orderScheduler.scheduleTaskWithDelay(() => makeOrderReady(cookingOrder.get), cookingDurationInSeconds)
    }

    chefsPromise.future
  }

  protected def makeOrderReady(cookingOrder: Order): Try[Order] = Try {
    Order.readyForPickup(cookingOrder) match {
      case Failure(ex) => throw SimulatorException(s"Failed to make order ready for pickup: $cookingOrder", ex)
      case Success(readyOrder) => {
        //blocking operation
        orderQueue.put(readyOrder)
        logger.info(s"Chef cooked order: $readyOrder")
        readyOrder
      }
    }
  }

  def dispatchCourier(orderNotification: OrderNotification): Future[Try[Courier]] = {
    val orderId = simulatorConfig.matchStrategy match {
      case FifoMatchStrategy() => None
      case OrderIdMatchStrategy() => Some(orderNotification.id)
      case x => throw SimulatorException(s"Courier dispatch is not supported for: $x")
    }

    val dispatchedCourier = Courier.dispatchNewCourier(orderId, transitDuration = Duration.ofSeconds(Random.between(3, 16)))
    val arrivalDelayInSeconds = dispatchedCourier.transitDuration.getSeconds

    val couriersPromise = courierDispatchScheduler.scheduleTaskWithDelay(() => courierArrived(dispatchedCourier), arrivalDelayInSeconds)
    couriersPromise.future
  }

  protected def courierArrived(dispatchedCourier: Courier): Try[Courier] = Try {
    Courier.arrived(dispatchedCourier) match {
      case Failure(ex) => throw SimulatorException(s"Courier failed to arrive: $dispatchedCourier", ex)
      case Success(arrivedCourier) => {
        //blocking operation
        courierQueue.put(arrivedCourier)
        logger.info(s"Courier has arrived: $arrivedCourier")
        arrivedCourier
      }
    }
  }
}

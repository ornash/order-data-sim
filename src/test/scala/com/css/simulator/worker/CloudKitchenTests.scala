package com.css.simulator.worker

import java.util.concurrent.{LinkedBlockingQueue}

import com.css.simulator.SimulatorConfig
import com.css.simulator.model.{Courier, Order, OrderNotification}
import com.css.simulator.strategy.OrderIdMatchStrategy
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.Random

class CloudKitchenTests extends AnyFunSuite {
  val DEFAULT_SIM_CONFIG = SimulatorConfig.defaultSimulatorConfig()
  val PREP_TIME_IN_SECONDS = 1

  test("Cook order") {
    val orderQueue = new LinkedBlockingQueue[Order]
    val courierQueue = new LinkedBlockingQueue[Courier]
    val ck = CloudKitchen(DEFAULT_SIM_CONFIG, orderQueue, courierQueue)

    val orderNotification = OrderNotification(id = Random.nextInt().toString, name = "test", prepTime = PREP_TIME_IN_SECONDS)
    val futureReadyOrder = ck.cookOrder(orderNotification)
    val readyOrder = Await.result(futureReadyOrder,  Duration(PREP_TIME_IN_SECONDS + 1, SECONDS))
    assert(readyOrder.isSuccess)
    assert(readyOrder.get.currentStatus.isReady())
    assert(readyOrder.get.cookDuration().get.getSeconds >= PREP_TIME_IN_SECONDS)

    val readyOrderFromQueue = orderQueue.poll(PREP_TIME_IN_SECONDS + 1, SECONDS)
    assert(readyOrderFromQueue != null)
    assert(readyOrderFromQueue.currentStatus.isReady())
    assert(readyOrderFromQueue.cookDuration().get.getSeconds >= PREP_TIME_IN_SECONDS)
    assertResult(readyOrder.get.id)(readyOrderFromQueue.id)
  }

  test("Cook order failure") {
    val courierQueue = new LinkedBlockingQueue[Courier]
    val ck = CloudKitchen(DEFAULT_SIM_CONFIG, null, courierQueue)

    val orderNotification = OrderNotification(id = Random.nextInt().toString, name = "test", prepTime = PREP_TIME_IN_SECONDS)
    val futureReadyOrder = ck.cookOrder(orderNotification)
    val readyOrder = Await.result(futureReadyOrder,  Duration(PREP_TIME_IN_SECONDS + 1, SECONDS))
    assert(readyOrder.isFailure) //because orderQueue is null
  }

  test("Dispatch courier fifo strategy") {
    val orderQueue = new LinkedBlockingQueue[Order]
    val courierQueue = new LinkedBlockingQueue[Courier]
    val smallerRange = (1,2)
    val ck = CloudKitchen(DEFAULT_SIM_CONFIG.copy(dispatchDelayRange = smallerRange), orderQueue, courierQueue)

    val orderNotification = OrderNotification(id = Random.nextInt().toString, name = "test", prepTime = PREP_TIME_IN_SECONDS)
    val futureArrivedCourier = ck.dispatchCourier(orderNotification)
    val arrivedCourier = Await.result(futureArrivedCourier,  Duration(3, SECONDS))
    assert(arrivedCourier.isSuccess)
    assert(arrivedCourier.get.currentStatus.isArrived())
    assert(arrivedCourier.get.dispatchDuration().get.getSeconds >= 1)
    assert(arrivedCourier.get.orderId.isEmpty)

    val arrivedCourierFromQueue = courierQueue.poll(3, SECONDS)
    assert(arrivedCourierFromQueue != null)
    assert(arrivedCourierFromQueue.currentStatus.isArrived())
    assert(arrivedCourierFromQueue.dispatchDuration().get.getSeconds >= 1)
    assert(arrivedCourierFromQueue.orderId.isEmpty)
  }

  test("Dispatch courier order id match strategy") {
    val orderQueue = new LinkedBlockingQueue[Order]
    val courierQueue = new LinkedBlockingQueue[Courier]
    val smallerRange = (1,2)
    val ck = CloudKitchen(DEFAULT_SIM_CONFIG.copy(matchStrategy = OrderIdMatchStrategy(), dispatchDelayRange = smallerRange), orderQueue, courierQueue)

    val orderNotification = OrderNotification(id = Random.nextInt().toString, name = "test", prepTime = PREP_TIME_IN_SECONDS)
    val futureArrivedCourier = ck.dispatchCourier(orderNotification)
    val arrivedCourier = Await.result(futureArrivedCourier,  Duration(3, SECONDS))
    assert(arrivedCourier.isSuccess)
    assert(arrivedCourier.get.currentStatus.isArrived())
    assert(arrivedCourier.get.dispatchDuration().get.getSeconds >= 1)
    assertResult(orderNotification.id)(arrivedCourier.get.orderId.get)

    val arrivedCourierFromQueue = courierQueue.poll(3, SECONDS)
    assert(arrivedCourierFromQueue != null)
    assert(arrivedCourierFromQueue.currentStatus.isArrived())
    assert(arrivedCourierFromQueue.dispatchDuration().get.getSeconds >= 1)
    assertResult(orderNotification.id)(arrivedCourierFromQueue.orderId.get)
  }

  test("Dispatch courier failure") {
    val orderQueue = new LinkedBlockingQueue[Order]
    val smallerRange = (1,2)
    val ck = CloudKitchen(DEFAULT_SIM_CONFIG.copy(dispatchDelayRange = smallerRange), orderQueue, null)

    val orderNotification = OrderNotification(id = Random.nextInt().toString, name = "test", prepTime = PREP_TIME_IN_SECONDS)
    val futureArrivedCourier = ck.dispatchCourier(orderNotification)
    val arrivedCourier = Await.result(futureArrivedCourier,  Duration(3, SECONDS))
    assert(arrivedCourier.isFailure)
  }
}

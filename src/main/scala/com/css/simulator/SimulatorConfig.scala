package com.css.simulator

import com.css.simulator.strategy.{FifoMatchStrategy, MatchStrategy, OrderIdMatchStrategy}

import scala.io.StdIn._

/**
 * Configuration for [[com.css.simulator.DispatchSimulator]]
 */
case class SimulatorConfig(ordersFilePath: String,
                           orderReceiptSpeed: Int,
                           matchStrategy: MatchStrategy,
                           private val threadCounts: (Int, Int),
                           private val dispatchDelayRange: (Int, Int)) {
  val orderWorkerThreads = threadCounts._1
  val courierDispatchThreads = threadCounts._2
  val minDispatchDelay = dispatchDelayRange._1
  val maxDispatchDelay = dispatchDelayRange._2

}

object SimulatorConfig {
  val PROCESSOR_COUNT = Runtime.getRuntime.availableProcessors()
  val ORDER_PROCESSOR_THREAD_COUNT = PROCESSOR_COUNT / 2
  val COURIER_DISPATCH_THREAD_COUNT = PROCESSOR_COUNT / 2
  val MIN_DISPATCH_DELAY = 3
  val MAX_DISPATCH_DELAY = 15
  val DEFAULT_FILE = "./dispatch_orders.json"
  val DEFAULT_SPEED = 2

  def defaultSimulatorConfig(): SimulatorConfig = {
    val threadCounts = (ORDER_PROCESSOR_THREAD_COUNT, COURIER_DISPATCH_THREAD_COUNT)
    val dispatchDelayRange = (MIN_DISPATCH_DELAY, MAX_DISPATCH_DELAY)
    SimulatorConfig(DEFAULT_FILE, DEFAULT_SPEED, FifoMatchStrategy(), threadCounts, dispatchDelayRange)
  }

  def readSimulatorConfig(): SimulatorConfig = {
    var inputLine = readLine(s"Enter orders input file path [$DEFAULT_FILE]: ")
    val ordersFilePath = if (inputLine.isEmpty) s"$DEFAULT_FILE" else inputLine

    inputLine = readLine(s"Enter order receipt speed per second [$DEFAULT_SPEED]: ")
    val orderReceiptSpeed = if (inputLine.isEmpty) DEFAULT_SPEED else inputLine.toInt

    inputLine = readLine(s"Enter worker thread count for processing orders [$ORDER_PROCESSOR_THREAD_COUNT]: ")
    val orderWorkerThreads = if (inputLine.isEmpty) ORDER_PROCESSOR_THREAD_COUNT else inputLine.toInt
    inputLine = readLine(s"Enter worker thread count for dispatching couriers [$COURIER_DISPATCH_THREAD_COUNT]: ")
    val courierDispatchThreads = if (inputLine.isEmpty) COURIER_DISPATCH_THREAD_COUNT else inputLine.toInt
    val threadCounts = (orderWorkerThreads, courierDispatchThreads)

    inputLine = readLine(s"Enter minimum delay(in seconds) for courier dispatch [$MIN_DISPATCH_DELAY]: ")
    val minDispatchDelay = if (inputLine.isEmpty) MIN_DISPATCH_DELAY else inputLine.toInt
    inputLine = readLine(s"Enter maximum delay(in seconds) for courier dispatch [$MAX_DISPATCH_DELAY]: ")
    val maxDispatchDelay = if (inputLine.isEmpty) MAX_DISPATCH_DELAY else inputLine.toInt
    val dispatchDelayRange = (minDispatchDelay, maxDispatchDelay)

    inputLine = readLine(s"Enter 1 for FIFO match strategy or 2 for OrderId match strategy: [1]: ")
    val strategySelection = if (inputLine.isEmpty) 1 else inputLine.toInt
    val matchStrategy = if (strategySelection == 1) FifoMatchStrategy() else OrderIdMatchStrategy()

    SimulatorConfig(ordersFilePath, orderReceiptSpeed, matchStrategy, threadCounts, dispatchDelayRange)
  }
}
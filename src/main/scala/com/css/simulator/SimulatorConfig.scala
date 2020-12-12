package com.css.simulator

import com.css.simulator.strategy.{FifoMatchStrategy, MatchStrategy, OrderIdMatchStrategy}

import scala.io.StdIn._

case class SimulatorConfig(ordersFilePath: String,
                           orderReceiptSpeed: Int,
                           totalWorkerThreads: Int,
                           matchStrategy: MatchStrategy)

object SimulatorConfig {
  val THREAD_POOL_SIZE = Runtime.getRuntime.availableProcessors()

  def readSimulatorConfig(): SimulatorConfig = {
    var inputLine = readLine("Enter orders input file [./dispatch_orders.json]: ")
    val ordersFilePath = if (inputLine.isEmpty) "./dispatch_orders.json" else inputLine

    inputLine = readLine("Enter order receipt speed per second [2]: ")
    val orderReceiptSpeed = if (inputLine.isEmpty) 2 else inputLine.toInt

    inputLine = readLine(s"Enter worker thread count [$THREAD_POOL_SIZE]: ")
    val totalWorkerThreads = if (inputLine.isEmpty) THREAD_POOL_SIZE else inputLine.toInt

    inputLine = readLine(s"Enter 1 for FIFO match strategy or 2 for OrderId match strategy: [1]: ")
    val strategySelection = if (inputLine.isEmpty) 1 else inputLine.toInt

    val matchStrategy = if (strategySelection == 1) FifoMatchStrategy() else OrderIdMatchStrategy()
    SimulatorConfig(ordersFilePath, orderReceiptSpeed, totalWorkerThreads, matchStrategy)
  }
}
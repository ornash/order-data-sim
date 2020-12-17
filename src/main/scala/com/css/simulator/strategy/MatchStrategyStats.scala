package com.css.simulator.strategy

import com.typesafe.scalalogging.LazyLogging

/**
 * Statistics printer for given MatchStrategy of simulation.
 */
case class MatchStrategyStats(matchStrategy: MatchStrategy) extends LazyLogging {
  private val matchedOrders = matchStrategy.getMatchedOrders()
  private val matchedCouriers = matchStrategy.getMatchedCouriers()
  private val expectedPrepDurationStats = DurationStats(matchedOrders.map(_.prepDuration))
  private val actualPrepDurationStats = DurationStats(matchedOrders.map(_.cookDuration().get))
  private val orderWaitDurationStats = DurationStats(matchedOrders.map(_.waitDuration().get))

  private val expectedTransitDurationStats = DurationStats(matchedCouriers.map(_.transitDuration))
  private val actualTransitDurationStats = DurationStats(matchedCouriers.map(_.dispatchDuration().get))
  private val courierWaitDurationStats = DurationStats(matchedCouriers.map(_.waitDuration().get))

  def printStats(): Unit = {
    logger.info(s"Simulation stats for order-courier match strategy: $matchStrategy")
    logger.info(s"Expected order prepDuration stats:      $expectedPrepDurationStats")
    logger.info(s"Actual order prepDuration stats:        $actualPrepDurationStats")
    logger.info(s"Order match waitDuration stats:         $orderWaitDurationStats")
    logger.info("")
    logger.info(s"Expected courier transitDuration stats: $expectedTransitDurationStats")
    logger.info(s"Actual courier transitDuration stats:   $actualTransitDurationStats")
    logger.info(s"Courier match waitDuration stats:       $courierWaitDurationStats")
  }

  private case class DurationStats(durations: Seq[java.time.Duration]) {
    val total = durations.size
    val durationsInMilli = if (total != 0) durations.map(_.toMillis) else Seq.empty[Long]
    val avgMs = if (total != 0) durationsInMilli.foldLeft(0L)(_ + _) / total else 0
    val maxMs = durationsInMilli.maxOption.getOrElse(0)
    val minMs = durationsInMilli.minOption.getOrElse(0)
    val medianMs = if (durationsInMilli.nonEmpty) median(durationsInMilli) else 0

    override def toString: String = {
      s"Total=$total, Avg=${avgMs}ms, Median=${medianMs}ms, Max=${maxMs}ms, Min=${minMs}ms"
    }

    private def median(seq: Seq[Long]): Long = {
      val sortedSeq = seq.sortWith(_ < _)
      val midIndex = sortedSeq.size / 2
      val median = sortedSeq(midIndex)
      if (sortedSeq.size % 2 == 0) {
        (sortedSeq(midIndex - 1) + median) / 2
      } else {
        median
      }
    }
  }

}

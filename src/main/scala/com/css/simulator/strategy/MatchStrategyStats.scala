package com.css.simulator.strategy

import com.typesafe.scalalogging.LazyLogging

object MatchStrategyStats extends LazyLogging {

  def printAllMatches(matchStrategy: MatchStrategy): Unit = {
    logger.whenDebugEnabled({
      matchStrategy.getMatchedOrderAndCouriers().foreach(orderAndCourier => logger.debug(s"Match = $orderAndCourier"))
    })
  }

  def printStats(matchStrategy: MatchStrategy): Unit = {
    printStats("Order Receipt", matchStrategy.getMatchedOrders.map(_.receivedDuration().get))
    printStats("Expected Order Prep", matchStrategy.getMatchedOrders.map(_.prepDuration))
    printStats("Order Cooking", matchStrategy.getMatchedOrders.map(_.cookDuration().get))
    printStats("Order Wait", matchStrategy.getMatchedOrders.map(_.waitDuration().get))

    logger.info("")

    printStats("Expected Courier ArrivalDelay", matchStrategy.getMatchedCouriers.map(_.transitDuration))
    printStats("Courier Dispatch", matchStrategy.getMatchedCouriers.map(_.dispatchDuration().get))
    printStats("Courier Wait", matchStrategy.getMatchedCouriers.map(_.waitDuration().get))
  }

  //TODO: write this to a csv for analysis
  def printStats(of: String, durations: Seq[java.time.Duration]): Unit = {
    val total = durations.size
    val avgMs = if(total != 0) durations.map(_.toMillis).foldLeft(0L)(_ + _) / total else total
    val maxMs = durations.map(_.toMillis).maxOption.getOrElse(0)
    val minMs = durations.map(_.toMillis).minOption.getOrElse(0)
    val medianMs = if(durations.nonEmpty) medianCalculator(durations.map(_.toMillis)) else 0
    val indentString = " " * (30 - of.length)
    logger.info(s"$of Stats: $indentString Total=$total, Avg=$avgMs, Median=$medianMs, Max=$maxMs, Min=$minMs")
  }

  def medianCalculator(seq: Seq[Long]): Long = {
    //In order if you are not sure that 'seq' is sorted
    val sortedSeq = seq.sortWith(_ < _)

    if (seq.size % 2 == 1) {
      sortedSeq(sortedSeq.size / 2)
    } else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      (up.last + down.head) / 2
    }
  }
}

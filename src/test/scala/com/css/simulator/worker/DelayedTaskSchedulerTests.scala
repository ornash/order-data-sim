package com.css.simulator.worker

import java.util.concurrent.TimeUnit

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.MILLISECONDS

class DelayedTaskSchedulerTests extends AnyFunSuite {
  val DELAY_IN_SECONDS = 2

  test("Delay execution for a duration") {
    val startTime = System.currentTimeMillis()

    val delayedTaskScheduler = DelayedTaskScheduler(1)
    val timeDiffPromise = delayedTaskScheduler.scheduleTaskWithDelay(() => {
      System.currentTimeMillis() - startTime
    }, DELAY_IN_SECONDS)

    //Await result only for 3 seconds
    val timeDiffInMilli = Await.result(timeDiffPromise.future, Duration(DELAY_IN_SECONDS + 1, SECONDS))
    assert(timeDiffInMilli >= (DELAY_IN_SECONDS * 1000))
  }

  test("Delayed execution with result") {
    val tenMilliDelay = 10
    val expectedResult = "x"

    val delayedTaskScheduler = DelayedTaskScheduler(1)
    val resultPromise = delayedTaskScheduler.scheduleTaskWithDelay(() => {
      expectedResult
    }, tenMilliDelay, TimeUnit.MILLISECONDS)

    //Await result only for twice the delayed time
    val result = Await.result(resultPromise.future, Duration(tenMilliDelay * 2, MILLISECONDS))
    assertResult(expectedResult)(result)
  }
}

package com.css.simulator.worker

import java.time.Duration
import java.util.concurrent.{Executors, TimeUnit}

import scala.concurrent.Promise
import scala.util.Try

case class DelayedTaskScheduler(threadCount: Int) {
  private val scheduledThreadPool = Executors.newScheduledThreadPool(threadCount)

  private def runnableTaskWithPromise[T](taskLambda: () => T, taskPromise: Promise[T]) : Runnable = {
    new Runnable {
      override def run(): Unit = {
        taskPromise.complete(Try(taskLambda()))
      }
    }
  }

  def scheduleTaskWithDelay[T](taskLambda: () => T, delay: Long, timeUnit: TimeUnit = TimeUnit.SECONDS): Promise[T] = {
    val completionPromise = Promise[T]()
    scheduledThreadPool.schedule(runnableTaskWithPromise(taskLambda, completionPromise), delay, timeUnit)
    completionPromise
  }
}

package com.css.simulator.worker

import java.util.concurrent.{Executors, TimeUnit}

import scala.concurrent.Promise
import scala.util.Try

/**
 * Scheduler that executes task/lambda after specified delay.
 *
 * Implemented using [[java.util.concurrent.ScheduledThreadPoolExecutor]].
 * Total number of worker threads in pool is controlled by threadCount specified during creation of DelayedTaskScheduler.
 */
case class DelayedTaskScheduler(threadCount: Int) {
  private val scheduledThreadPool = Executors.newScheduledThreadPool(threadCount)

  private def runnableTaskWithPromise[T](taskLambda: () => T, taskPromise: Promise[T]) : Runnable = {
    new Runnable {
      override def run(): Unit = {
        taskPromise.complete(Try(taskLambda()))
      }
    }
  }

  /**
   * @param taskLambda lambda/task to be executed
   * @param delay delay before execution of lambda/task
   * @param timeUnit TimeUnit of delay, defaults to time in seconds
   * @tparam T DataType of result returned by taskLambda
   * @return [[scala.concurrent.Promise]] that a result will be returned after task is executed.
   *        The result can be accessed through the future.
   */
  def scheduleTaskWithDelay[T](taskLambda: () => T, delay: Long, timeUnit: TimeUnit = TimeUnit.SECONDS): Promise[T] = {
    val completionPromise = Promise[T]()
    scheduledThreadPool.schedule(runnableTaskWithPromise(taskLambda, completionPromise), delay, timeUnit)
    completionPromise
  }
}

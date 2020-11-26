package com.css.worker

import java.util.concurrent.LinkedBlockingDeque

import com.css.model.{Courier, OrderNotification}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class CourierDispatcher(ec: ExecutionContext, courierQueue: LinkedBlockingDeque[Courier]) extends LazyLogging {

  def dispatchCourier(orderNotification: OrderNotification): Future[Courier] = {
    val dispatchedCourier = Courier.dispatchNewCourier(Option(orderNotification.id))

    val dispatcher = Future {
      Thread.sleep(dispatchedCourier.arrivalDelayDuration.toMillis)
      val arrivedCourier = Courier.arrived(dispatchedCourier)
      //blocking operation
      courierQueue.put(arrivedCourier)
      arrivedCourier
    }(ec)

    dispatcher.onComplete {
      case Success(_) => logger.info(s"Dispatcher sent courier: $dispatchedCourier")
      case Failure(t) => logger.error(s"Dispatcher failed to send courier: $dispatchedCourier", t)
    }(ec)

    dispatcher
  }
}

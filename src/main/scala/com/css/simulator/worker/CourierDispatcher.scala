package com.css.simulator.worker

import java.util.concurrent.LinkedBlockingQueue

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.{Courier, OrderNotification}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class CourierDispatcher(ec: ExecutionContext, courierQueue: LinkedBlockingQueue[Courier]) extends LazyLogging {

  def dispatchCourier(orderNotification: OrderNotification): Future[Courier] = {
    val dispatchedCourier = Courier.dispatchNewCourier(Option(orderNotification.id))

    val dispatcher = Future {
      Thread.sleep(dispatchedCourier.arrivalDelayDuration.toMillis)

      Courier.arrived(dispatchedCourier) match {
        case Failure(ex) => throw SimulatorException(s"Courier failed to arrive: $dispatchedCourier", ex)
        case Success(arrivedCourier) => {
          //blocking operation
          courierQueue.put(arrivedCourier)
          //logger.info(s"Courier has arrived: $arrivedCourier")
          arrivedCourier
        }
      }
    }(ec)

//    dispatcher.onComplete {
//      case Success(arrivedCourier) => logger.info(s"Dispatcher sent courier: $arrivedCourier")
//      case Failure(t) => logger.error(s"Dispatcher failed to send courier: $dispatchedCourier", t)
//    }(ec)

    dispatcher
  }
}

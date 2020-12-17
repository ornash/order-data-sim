package com.css.simulator.reader

import com.css.simulator.exception.SimulatorException
import com.css.simulator.model.OrderNotification
import com.typesafe.scalalogging.LazyLogging
import upickle.default._

import scala.io.Source
import scala.reflect.io.Path
import scala.util.{Failure, Success, Try}

object OrderNotificationReader extends LazyLogging {

  def attemptReadOrdersFile(filePath: Path): Try[Seq[OrderNotification]] = {
    try {

      logger.info(s"Reading orders json file at: ${filePath.toAbsolute.path}")

      val jsonFile = Source.fromFile(filePath.toAbsolute.path, "utf-8")
      val jsonContent = try { jsonFile.getLines.mkString } finally { jsonFile.close() }

      val json = ujson.read(jsonContent)
      if(json.isNull || json.arrOpt.isEmpty || json.arrOpt.get.isEmpty) {
        val errorMsg = s"Orders json file at: $filePath is empty."
        logger.error(errorMsg)
        Failure(SimulatorException(errorMsg))
      } else {
        val allOrders = json.arr.map(orderJson => read[OrderNotification](orderJson)).toSeq
        logger.info("All orders read successfully.")
        Success(allOrders)
      }

    } catch {
      case e: Exception => {
        val errorMsg = s"Failed to read orders json file at: $filePath"
        logger.error(errorMsg, e)
        Failure(SimulatorException(errorMsg, e))
      }
    }
  }
}
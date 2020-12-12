package com.css.simulator.model

import upickle.default.{ReadWriter, macroRW}

/**
 * An order notification
 *
 * @param id order's unique id
 * @param name order's name
 * @param prepTime order's total preparation time in seconds.
 */
case class OrderNotification(id: String, name: String = "", prepTime: Int) {
  require(Option(id).isDefined, "id cannot be null.")
  require(prepTime < Int.MaxValue, s"prepTime cannot be greater than or equal to ${Int.MaxValue} for order: $id")
  require(prepTime >= 0, s"prepTime cannot be negative for order: $id")
}

object OrderNotification {
  /**
   * JSON reader-writer for OrderNotification.
   */
  implicit val rw: ReadWriter[OrderNotification] = macroRW
}

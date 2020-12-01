package com.css.simulator.model

import upickle.default.{ReadWriter, macroRW}

case class OrderNotification(id: String, name: String = "", prepTime: Int) {
  require(Option(id).isDefined, "id cannot be null.")
  require(prepTime < Int.MaxValue, s"prepTime cannot be greater than or equal to ${Int.MaxValue} for order: $id")
  require(prepTime >= 0, s"prepTime cannot be negative for order: $id")
}

object OrderNotification {
  implicit val rw: ReadWriter[OrderNotification] = macroRW
}

package com.css.simulator.model

import upickle.default._
import org.scalatest.funsuite.AnyFunSuite

class OrderNotificationTestSuite extends AnyFunSuite {

  test("Constructing OrderNotification without id should throw IllegalArgumentException") {
    assertThrows[IllegalArgumentException] {
      OrderNotification(id = null, name = "test", prepTime = 1)
    }
  }

  test("Constructing OrderNotification with negative prepTime should throw IllegalArgumentException") {
    assertThrows[IllegalArgumentException] {
      OrderNotification(id = "", name = "test", prepTime = -1)
    }
  }

  test("Constructing OrderNotification with large prepTime should throw IllegalArgumentException") {
    assertThrows[IllegalArgumentException] {
      OrderNotification(id = "", name = "test", prepTime = Int.MaxValue)
    }
  }

  test("Constructing OrderNotification without name should succeed") {
    assert(Option(OrderNotification(id = "0ff534a7-a7c4-48ad-b6ec-7632e36af950", prepTime = 7)).isDefined)
  }

  test("Constructing OrderNotification by parsing json string should succeed") {
    val testOrder = read[OrderNotification]("""{"id": "0ff534a7-a7c4-48ad-b6ec-7632e36af950","name": "Cheese Pizza","prepTime": 13}""")
    assert(Option(testOrder).isDefined)
    assert(testOrder.id == "0ff534a7-a7c4-48ad-b6ec-7632e36af950")
    assert(testOrder.name == "Cheese Pizza")
    assert(testOrder.prepTime == 13)
  }

  test("Parsing invalid json string should throw exception") {
    assertThrows[Exception] {
      read[OrderNotification]("""{"id": "0ff534a7-a7c4-48ad-b6ec-7632e36af950","name": "Cheese Pizza"}""")
    }
  }
}
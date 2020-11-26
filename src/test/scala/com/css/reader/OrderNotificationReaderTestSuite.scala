package com.css.reader

import com.css.exception.SimulatorException
import org.scalatest.funsuite.AnyFunSuite

class OrderNotificationReaderTestSuite extends AnyFunSuite {

  test("OrderNotificationReader reads all content") {
    val readOrdersAttempt = OrderNotificationReader.attemptReadOrdersFile("./src/test/resources/dispatch_orders_test.json")
    assert(readOrdersAttempt.isSuccess)
    assert(readOrdersAttempt.get.size == 3)
  }

  test("OrderNotificationReader fails if file doesnt exist") {
    val readOrdersAttempt = OrderNotificationReader.attemptReadOrdersFile("./src/test/resources/doesnt_exist.json")
    assert(readOrdersAttempt.isFailure)
    assert(readOrdersAttempt.failed.get.isInstanceOf[SimulatorException])
  }

  test("OrderNotificationReader fails if file is invalid") {
    val readOrdersAttempt = OrderNotificationReader.attemptReadOrdersFile("./src/test/resources/invalid.json")
    assert(readOrdersAttempt.isFailure)
    assert(readOrdersAttempt.failed.get.isInstanceOf[SimulatorException])
  }

  test("OrderNotificationReader fails if file is empty") {
    val readOrdersAttempt = OrderNotificationReader.attemptReadOrdersFile("./src/test/resources/empty.json")
    assert(readOrdersAttempt.isFailure)
    assert(readOrdersAttempt.failed.get.isInstanceOf[SimulatorException])
  }
}
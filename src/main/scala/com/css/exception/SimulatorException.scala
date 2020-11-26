package com.css.exception

case class SimulatorException(private val message: String = "",
                              private val cause: Throwable = None.orNull) extends Exception(message, cause)

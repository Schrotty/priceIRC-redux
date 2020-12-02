package de.rubenmaurer.price.util

import org.apache.commons.lang.StringUtils
import org.fusesource.jansi.Ansi._

object TerminalHelper {
  val Success = "SUCCESS"
  val Pending = "PROCESSING"
  val FAILURE = "FAILURE"

  private val TerminalSize = 80

  def displayStartup(): Unit = {
    println(divider)
    println(centerAndWrap(TemplateManager.getStartupMessage))
    println(centerAndWrap(TemplateManager.getVersionString))
    println(centerAndWrap(TemplateManager.getRuntimeID))
    println(divider)
  }

  def displayTestSuite(suite: String): Unit = {
    println(centerAndWrap("=== " + suite + " ==="))
    println(divider)
  }

  def displayTestSuiteResult(): Unit = {
    println(divider)
  }

  def displayConnectionFailure(address: String): Unit = {
    println(divider)
    println(centerAndWrap(TemplateManager.getConnectionFailure(address)))
    println(divider)
  }

  def displayTestStatus(test: String, status: String, finish: Boolean = false): Unit = {
    val emptySpace = " ".repeat(TerminalSize - (test.length + status.length) - 2)
    var finStatus = status

    status match {
      case Success => finStatus = ansi().fgGreen().render(status).fgDefault().toString
      case Pending => finStatus = ansi().fgBlue().render(status).fgDefault().toString
      case FAILURE => finStatus = ansi().fgRed().render(status).fgDefault().toString
    }

    printf("%s%s%s |%s", leftCage(test), emptySpace, finStatus, if (finish) "\r\n" else "\r")
  }

  private def divider: String = "+" + "-".repeat(TerminalSize) + "+"
  private def center(input: String): String = StringUtils.center(input, TerminalSize)
  private def centerAndWrap(input: String): String = "|" + center(input) + "|"

  private def leftCage(input: String): String = "| " + input
}

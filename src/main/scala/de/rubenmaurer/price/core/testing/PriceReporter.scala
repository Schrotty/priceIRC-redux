package de.rubenmaurer.price.core.testing

import de.rubenmaurer.price.util.TerminalHelper
import org.scalatest.Reporter
import org.scalatest.events._

class PriceReporter(testCount: Integer) extends Reporter {
  private var _currentSuite: String = _
  private var _testCount = testCount

  def apply(event: Event): Unit = {
    event match {
      case TestStarting(ordinal, suiteName, suiteId, suiteClassName, testName, testText, formatter, location, rerunner, payload, threadName, timeStamp) =>
        if (_currentSuite != suiteName) this.apply(SuiteStarting(ordinal, suiteName, suiteName, suiteClassName))
        TerminalHelper.displayTestStatus(testName, TerminalHelper.Pending)

      case TestSucceeded(ordinal, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        _testCount = _testCount - 1
        TerminalHelper.displayTestStatus(testName, TerminalHelper.Success, finish = true)

        if (_testCount == 0) this.apply(SuiteCompleted(ordinal, suiteName, suiteName, suiteClassName))

      case TestFailed(ordinal, message, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, analysis, throwable, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        _testCount = _testCount - 1
        TerminalHelper.displayTestStatus(testName, TerminalHelper.FAILURE, finish = true)

      case ev => print()
    }
  }
}

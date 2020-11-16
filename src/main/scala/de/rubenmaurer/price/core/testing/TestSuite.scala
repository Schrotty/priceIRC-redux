package de.rubenmaurer.price.core.testing

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import de.rubenmaurer.price.core._
import de.rubenmaurer.price.core.facade.{Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite
import de.rubenmaurer.price.util.TerminalHelper
import org.scalatest.Args

object TestSuite {
  def apply(ts: String): Behavior[Command] =
    Behaviors.setup{ context =>
      val suite: BaseTestSuite = Class.forName(String.format("de.rubenmaurer.price.test.%s", ts)).getDeclaredConstructor(classOf[Session], classOf[Parser], classOf[String]).newInstance(Session.facade, Parser.facade, ts).asInstanceOf[BaseTestSuite]
      var tests: Set[String] = suite.testNames

      context.watch(context.spawn(Session.apply(context.self), "session"))
      context.watch(context.spawn(Parser.apply(), "parser"))

      context.self ! RunTestSuite
      Behaviors.receive[Command] { (context, message) =>
        message match {
          case RunTestSuite =>
            context.log.debug(s"Execute TestSuite ${suite.suiteName}")
            TerminalHelper.displayTestSuite(suite.suiteName)
            context.self ! RunSingleTest
            Behaviors.same

          case RunSingleTest =>
            context.log.debug(s"Execute Single Test ${tests.head}")
            suite.runTests(Option(tests.head), Args.apply(reporter = new PriceReporter(suite.testNames.size)))
            tests = tests.drop(1)
            Behaviors.same

          case SingleTestFinished =>
            if (tests.nonEmpty) context.self ! RunSingleTest else context.self ! AllTestsFinished
            Behaviors.same

          case AllTestsFinished =>
            TerminalHelper.displayTestSuiteResult()
            Behaviors.stopped

          case _ =>
            Behaviors.same
        }
      }
    }
}
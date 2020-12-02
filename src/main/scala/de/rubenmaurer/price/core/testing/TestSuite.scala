package de.rubenmaurer.price.core.testing

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import de.rubenmaurer.price.core.facade.Session.{ConnectionError, TestFinished}
import de.rubenmaurer.price.core.facade.{Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite
import de.rubenmaurer.price.util.{Configuration, TerminalHelper}
import org.scalatest.Args

object TestSuite {

  sealed trait Request
  final case object Execute extends Request
  final case object RunTest extends Request

  sealed trait Response
  final case class SuiteFailure(message: Any) extends Response
  final case object SuiteSuccess extends Response

  private final case class WrappedSessionResponse(response: Session.Response) extends Response with Request

  def apply(ts: String, parent: ActorRef[Response]): Behavior[Request] =
    Behaviors.setup{ context =>
      val sessionMapper: ActorRef[Session.Response] = context.messageAdapter(replyTo => WrappedSessionResponse(replyTo))

      val suite: BaseTestSuite = Class.forName(String.format("de.rubenmaurer.price.test.%s", ts)).getDeclaredConstructor(classOf[Session], classOf[Parser], classOf[String]).newInstance(Session.facade, Parser.facade, ts).asInstanceOf[BaseTestSuite]
      var tests: Set[String] = suite.testNames

      context.watch(context.spawn(Session.apply(sessionMapper), "session"))
      context.watch(context.spawn(Parser.apply(), "parser"))

      Behaviors.receive[Request] { (context, message) =>
        message match {
          case Execute =>
            TerminalHelper.displayTestSuite(suite.suiteName)
            context.self ! RunTest
            Behaviors.same

          case RunTest =>
            suite.runTests(Option(tests.head), Args.apply(reporter = new PriceReporter(suite.testNames.size)))
            tests = tests.drop(1)
            Behaviors.same

          case wrapped: WrappedSessionResponse =>
            wrapped.response match {
              case connectionError: ConnectionError =>
                TerminalHelper.displayConnectionFailure(s"${Configuration.hostname()}:${Configuration.port()}")
                parent ! SuiteFailure(connectionError.failureMessage.toString())

                Behaviors.stopped

              case TestFinished =>
                if (tests.nonEmpty) context.self ! RunTest
                else {
                  TerminalHelper.displayTestSuiteResult()
                  parent ! SuiteSuccess

                  Behaviors.stopped
                }

                Behaviors.same

              case _ => Behaviors.same
            }
        }
      }
    }
}
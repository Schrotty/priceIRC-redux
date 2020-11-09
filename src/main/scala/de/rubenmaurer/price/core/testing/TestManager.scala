package de.rubenmaurer.price.core.testing

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, Terminated}
import de.rubenmaurer.price.core.Command
import de.rubenmaurer.price.test.TestIndex
import de.rubenmaurer.price.util.Configuration

object TestManager {

  case class Execute() extends Command
  case class Finished() extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.debug("TestManager started!")

      var testSuites: List[String] = TestIndex.getAll(Configuration.tests())
      def spawnTestSuite(): Behavior[Command] = {
          context.log.debug("Spawn test suite")
          context.watchWith(context.spawnAnonymous(TestSuite(testSuites.head)), Finished())
          testSuites = testSuites.filter(s => !s.equals(testSuites.head))
          Behaviors.same
      }

      spawnTestSuite()
      Behaviors.receive[Command] { (context, message) =>
        context.log.debug(s"Received $message!")
        message match {
          case Finished() =>
            if (testSuites.nonEmpty) spawnTestSuite() else Behaviors.stopped

          case _ =>
            Behaviors.same
        }
      }.receiveSignal {
          case (context, Terminated(ref)) =>
            context.log.debug("Actor stopped: {}", ref.path.name)
            Behaviors.stopped
      }
    }
}


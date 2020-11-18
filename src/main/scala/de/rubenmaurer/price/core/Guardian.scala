package de.rubenmaurer.price.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import de.rubenmaurer.price.core.testing.TestManager
import de.rubenmaurer.price.core.testing.TestManager.{Execute, Finished}
import de.rubenmaurer.price.util.{Configuration, TerminalHelper}

object Guardian {
  def apply(args: Array[String]): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.debug("Guardian started!")

      //spawn children
      val testManager = context.spawn(TestManager(), "test-manager")

      //watch children
      context.watchWith(testManager, Finished())

      //print startup message
      TerminalHelper.displayStartup()

      //issue startup commands
      testManager ! Execute()

      Behaviors.receive[Command] { (_, message) =>
        message match {
          case Finished() =>
            Behaviors.stopped

          case _ =>
            Behaviors.same
        }
      }
    }
}
package de.rubenmaurer.price.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object TerminalManager {

  case class Info(message: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.debug("TerminalManager started!")

      Behaviors.receive { (context, message) =>
        message match {
          case Info(message) =>
            println(s"[ INFO ] $message")
            Behaviors.same

          case _ =>
            Behaviors.same
        }
      }
    }
}

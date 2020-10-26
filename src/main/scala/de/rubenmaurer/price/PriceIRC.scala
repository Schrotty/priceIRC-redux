package de.rubenmaurer.price

import akka.actor.typed.ActorSystem
import de.rubenmaurer.price.core.{Command, Guardian}

object PriceIRC {
  def main(args: Array[String]): Unit = {
    ActorSystem[Command](Guardian(args), "guardian")
  }
}

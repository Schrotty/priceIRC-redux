package de.rubenmaurer.price

import akka.actor.typed.ActorSystem
import de.rubenmaurer.price.core.{Command, Guardian}

object PriceIRC {
  var system: ActorSystem[_] = _

  def main(args: Array[String]): Unit = {
    system = ActorSystem[Command](Guardian(args), "guardian")
  }
}

package de.rubenmaurer.price

import akka.actor.typed.ActorSystem
import de.rubenmaurer.price.core.Guardian
import org.slf4j.LoggerFactory

object PriceIRC {
  var system: ActorSystem[_] = _

  def main(args: Array[String]): Unit = {
    LoggerFactory.getLogger("system").info("Starting...")

    system = ActorSystem[Guardian.Command](Guardian(args), "guardian")
  }
}

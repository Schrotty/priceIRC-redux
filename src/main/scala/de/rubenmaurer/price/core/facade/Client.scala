package de.rubenmaurer.price.core.facade

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import de.rubenmaurer.price.core.{Command, RegisterListener}

object Client {
  def CHLOE: Client = new Client("chloe", "elisabeth", "Chloe Elisabeth Price")
  def MAX: Client = new Client("max", "maxine", "Maxine Caulfield")
  def RACHEl: Client = new Client("rachel", "ramber", "Rachel Amber")
  def KATE: Client = new Client("kate", "bunny_mommy", "Kate Marsh")

  def apply(client: Client): Behavior[Command] = {
    Behaviors.setup { context =>
      var connectionHandler: ActorRef[Command] = context.system.ignoreRef

      Behaviors.receive[Command] { (context, message) =>
        message match {
          case RegisterListener(actorRef) =>
            connectionHandler = actorRef
            Behaviors.same

          case _ => Behaviors.same
        }
      }
    }
  }
}

class Client(var nickname: String, val username: String, val fullName: String) {
  private var _intern: ActorRef[Command] = _

  def intern: ActorRef[Command] = _intern
  def intern_= (ref: ActorRef[Command]):Unit = _intern = ref

  def link(actorRef: ActorRef[Command]): Unit = {
    intern = actorRef
  }

  def linked: Boolean = intern != null
}
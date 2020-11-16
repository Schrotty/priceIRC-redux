package de.rubenmaurer.price.core.facade

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.core._
import de.rubenmaurer.price.core.facade.Session.facade.timeout
import de.rubenmaurer.price.core.networking.ConnectionHandler

import scala.concurrent.{Await, ExecutionContextExecutor}

object Client {
  var tmpSender: ActorRef[Reply[Any]] =  _

  def CHLOE: Client = new Client("chloe", "elisabeth", "Chloe Elisabeth Price")
  def MAX: Client = new Client("max", "maxine", "Maxine Caulfield")
  def RACHEl: Client = new Client("rachel", "ramber", "Rachel Amber")
  def KATE: Client = new Client("kate", "bunny_mommy", "Kate Marsh")

  def apply(client: Client): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.debug("Client spawned!")
      var connectionHandler: ActorRef[Command] = context.actorOf(ConnectionHandler.apply(context.self)).toTyped

      Behaviors.receive[Command] { (context, message) =>
        context.log.debug("Received message: " + message)

        message match {
          case Request(command, sender) =>
            context.log.debug("Received command: " + command)

            command match {
              case RegisterListener(actorRef) =>
                connectionHandler = actorRef
                sender ! Reply(true, sender=context.self)
                Behaviors.same

              case Send(_, _) =>
                context.log.debug("Pre-Send")
                implicit val system: ActorSystem[_] = context.system
                implicit val ec: ExecutionContextExecutor = system.executionContext

                tmpSender = sender
                connectionHandler ! command

                Behaviors.same
            }

          case Reply(message, _) =>
            tmpSender ! Reply(message, context.self)
            Behaviors.same

          case _ => Behaviors.same
        }
      }
    }
  }
}

class Client(var nickname: String, val username: String, val fullName: String) {
  object log {
    var codes: Map[Int, String] = Map[Int, String]()
    var plain: List[String] = List[String]()

    /* === plain log methods === */
    def last: String = plain.head
    def startWith(input: String): String = plain.find(_.startsWith(input)).getOrElse("")

    /* === code log methods === */
    def byCode(code: Int): String = codes.apply(code)
  }

  private var _intern: ActorRef[Command] = _
  def intern: ActorRef[Command] = _intern

  def linked: Boolean = intern != null
  def link(actorRef: ActorRef[Command]): Unit = {
    _intern = actorRef
  }

  def send(message: String, expected: Int = 0): Unit = {
    try {
      implicit val system: ActorSystem[_] = PriceIRC.system
      implicit val ec: ExecutionContextExecutor = system.executionContext

      val reply: Reply[String] = Await.result(intern.ask[Reply[String]](pre => {
        Request(Send(message, expected), pre)
      }), timeout.duration)

      val code = """(\d{3})(.+)""".r
      for (line <- reply.payload.split("\r\n")) {
        line match {
          case code(code, message) => log.codes = log.codes + (code.toInt -> message)
          case _ => log.plain = reply.payload :: log.plain
        }
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}
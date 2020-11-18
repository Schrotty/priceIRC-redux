package de.rubenmaurer.price.core.facade

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.util.{Failure, Success}
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.core.facade.Client.{Command, Request, Response, SendMessage}
import de.rubenmaurer.price.core.facade.Session.facade.timeout
import de.rubenmaurer.price.core.networking.ConnectionHandler

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

object Client {
  sealed trait Command

  case class Request(command: Command, replyTo: ActorRef[Response]) extends Command
  case class Response(payload: String) extends Command
  case class SendMessage(message: String, expected: Int) extends Command

  private case class AdaptedResponse(message: String, replyTo: ActorRef[Response]) extends Command
  private case class WrappedConnectionResponse(response: ConnectionHandler.Response) extends Command

  def CHLOE: Client = new Client("chloe", "elisabeth", "Chloe Elisabeth Price")
  def MAX: Client = new Client("max", "maxine", "Maxine Caulfield")
  def RACHEl: Client = new Client("rachel", "ramber", "Rachel Amber")
  def KATE: Client = new Client("kate", "bunny_mommy", "Kate Marsh")

  def apply(client: Client): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.debug(s"Spawned ${client.username}")
      val connectionMapper: ActorRef[ConnectionHandler.Response] = context.messageAdapter(rsp => WrappedConnectionResponse(rsp))
      val connectionHandler: ActorRef[ConnectionHandler.Request] = context.actorOf(ConnectionHandler.apply(connectionMapper)).toTyped

      Behaviors.receive[Command] { (context, message) =>
        message match {
          case Request(command, replyTo) =>
            command match {
              case SendMessage(message, expected) =>
                implicit val timeout: Timeout = 3.seconds
                context.ask[ConnectionHandler.Request, ConnectionHandler.Response](connectionHandler, _ => ConnectionHandler.Send(message, expected, connectionMapper)) {
                  case Success(response: ConnectionHandler.Received) => AdaptedResponse(response.payload, replyTo)
                  case Failure(_) => AdaptedResponse("Request failed!", replyTo)
                }

                Behaviors.same
            }

          case AdaptedResponse(message, replyTo) =>
            replyTo ! Response(message)
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

      val reply: Response = Await.result(intern.ask[Response](pre => {
        Request(SendMessage(message, expected), pre)
      }), timeout.duration)

      val code = """(\d{3})(.+)""".r
      for (line <- reply.payload.split("\r\n")) {
        line match {
          case code(code, message) => log.codes = log.codes + (code.toInt -> message)
          case _ => log.plain = reply.payload :: log.plain
        }
      }
    } catch {
      case e: Exception =>
    }
  }
}
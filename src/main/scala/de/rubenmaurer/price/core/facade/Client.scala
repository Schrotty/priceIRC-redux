package de.rubenmaurer.price.core.facade

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.core.facade.Client.{Command, Request, Response, SendMessage}
import de.rubenmaurer.price.core.facade.Session.facade.timeout
import de.rubenmaurer.price.core.networking.ConnectionHandler
import de.rubenmaurer.price.util.TemplateManager

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, TimeoutException}

object Client {
  sealed trait Command

  case class Request(command: Command, replyTo: ActorRef[Response]) extends Command
  case class Response(payload: String) extends Command
  case class SendMessage(message: String, expected: Int) extends Command

  private case class WrappedConnectionResponse(response: ConnectionHandler.Response) extends Command

  def CHLOE: Client = new Client("chloe", "elisabeth", "Chloe Elisabeth Price")
  def MAX: Client = new Client("max", "maxine", "Maxine Caulfield")
  def RACHEl: Client = new Client("rachel", "ramber", "Rachel Amber")
  def KATE: Client = new Client("kate", "bunnymommy", "Kate Marsh")

  def apply(client: Client): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.debug(s"SPAWNED: ${client.username}")

      var response: List[String] = List()
      var expectedLines: Int = 0
      var tmpReplyTo: ActorRef[Response] = context.system.ignoreRef

      val responseMapper: ActorRef[ConnectionHandler.Response] = context.messageAdapter(rsp => WrappedConnectionResponse(rsp))
      val connectionHandler: ActorRef[ConnectionHandler.Request] = context.actorOf(ConnectionHandler.apply(responseMapper)).toTyped

      Behaviors.receive[Command] { (_, message) =>
        message match {
          case Request(command, replyTo) =>
            command match {
              case SendMessage(message, expected) =>
                response = List()
                expectedLines = expected
                tmpReplyTo = replyTo

                connectionHandler ! ConnectionHandler.Send(message, expected, responseMapper)
                Behaviors.same
            }

          case wrapped: WrappedConnectionResponse =>
            wrapped.response match {
              case ConnectionHandler.Received(payload) =>
                val rsp = payload.split("\r\n")
                response = rsp.toList ::: response

                if (response.length == expectedLines) tmpReplyTo ! Response(response.mkString("\r\n"))
                Behaviors.same

              case _ => Behaviors.same
            }

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
    def byCode(code: Int): String = codes.getOrElse(code, "")

    /* === utils === */
    def clearPlain(): Unit = plain = List()
  }

  private var _intern: ActorRef[Command] = _
  def intern: ActorRef[Command] = _intern

  def linked: Boolean = intern != null
  def link(actorRef: ActorRef[Command]): Unit = {
    _intern = actorRef
  }

  def authenticate(): Client = {
    send(TemplateManager.getNick(this.nickname))
    send(TemplateManager.getUser(this.username, this.fullName), 10)
    this
  }

  def authenticateWithSwapped(): Client = {
    send(TemplateManager.getUser(this.username, this.fullName))
    send(TemplateManager.getNick(this.nickname), 10)
    this
  }

  def authenticateWithWhitespace(): Client = {
    send(s"  ${TemplateManager.getNick(this.nickname)}  ")
    send(s"  ${TemplateManager.getUser(this.username, this.fullName)}  ", 10)
    this
  }

  def nick(expectResponse: Boolean = false): Client = {
    send(TemplateManager.getNick(this.nickname), if (expectResponse) 10 else 0)
    this
  }

  def user(expectResponse: Boolean = false): Client = {
    send(TemplateManager.getUser(this.username, this.fullName), if (expectResponse) 10 else 0)
    this
  }

  def whois(client: Client, shouldFail: Boolean = false): Unit = {
    send(TemplateManager.whois(client.nickname), if(shouldFail) 1 else 3)
  }

  def send(message: String): Unit = {
    implicit val system: ActorSystem[_] = PriceIRC.system
    implicit val ec: ExecutionContextExecutor = system.executionContext

    Session.logger.info(s"$nickname SEND -- $message")
    intern.ask[Response](replyTo => Request(SendMessage(message, 0), replyTo))
  }

  def send(message: String, expected: Int = 0): Unit = {
    try {
      implicit val system: ActorSystem[_] = PriceIRC.system
      implicit val timeout: Timeout = 3.seconds
      implicit val ec: ExecutionContextExecutor = system.executionContext

      //log.clearPlain()
      Session.logger.info(s"$nickname SEND -- $message")
      val reply: Response = Await.result(intern.ask[Response](replyTo => {
        Request(SendMessage(message, expected), replyTo)
      }), timeout.duration)

      val code = """(.+?)(\d{3})(.*)""".r
      for (line <- reply.payload.split("\r\n")) {
        line match {
          case code(_, command, _) =>
            log.codes = log.codes + (command.toInt -> line)

          case _ =>
            log.plain = line :: log.plain
        }

        Session.logger.info(s"$nickname RECV -- $line")
      }
    }
    catch
    {
      case e: TimeoutException => Session.logger.info(e.getMessage)
      case _: Throwable =>
    }
  }
}
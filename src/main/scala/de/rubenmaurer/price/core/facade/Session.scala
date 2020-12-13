package de.rubenmaurer.price.core.facade

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.io.Tcp.CommandFailed
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.core.facade.Session.{Request, SpawnClient, SpawnedClient}
import de.rubenmaurer.price.core.networking.ConnectionHandler
import de.rubenmaurer.price.util.Configuration
import de.rubenmaurer.price.util.Configuration.runtimeIdentifier
import org.slf4j.MDC

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.sys.process.{ProcessLogger, _}

object Session {
  sealed trait Request
  final case class SpawnClient(client: Client, replyTo: ActorRef[SpawnedClient]) extends Request
  final case object GetServerStatus extends Request
  final case object ReportStatus extends Request

  sealed trait Response
  final case class SpawnedClient(client: Client) extends Response
  final case class ConnectionError(failureMessage: CommandFailed) extends Response
  final case object TestFinished extends Response

  private case class WrappedConnectionResponse(response: ConnectionHandler.Response) extends Response with Request

  val facade: Session = new Session()
  val logger: Logger = Logger("test")

  def apply(suite: ActorRef[Response]): Behavior[Request] = {
    Behaviors.setup { context =>
      val connectionMapper: ActorRef[ConnectionHandler.Response] = context.messageAdapter(rsp => WrappedConnectionResponse(rsp))

      facade.intern = context.self
      Behaviors.receive[Request] { (context, message) =>
        message match {
          case GetServerStatus =>
            context.actorOf(ConnectionHandler.apply(connectionMapper)).toTyped
            Behaviors.same

          case SpawnClient(preset, replyTo) =>
            preset.link(context.spawn(Client(preset), preset.username))
            Thread.sleep(50)

            replyTo ! SpawnedClient(preset)
            Behaviors.same

          case ReportStatus =>
            context.children.foreach(f => context.stop(f))
            suite ! TestFinished
            Behaviors.same

          case wrapped: WrappedConnectionResponse =>
            wrapped.response match {
              case failure: ConnectionHandler.Failure =>
                facade.online = false
                suite ! ConnectionError(failure.failureMessage)
                Behaviors.stopped

              case success: ConnectionHandler.Success =>
                facade.online = true
                success.replyTo ! ConnectionHandler.Disconnect
                Behaviors.same
            }
        }
      }
    }
  }
}

class Session() {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  private var _process: Process = _
  private var _logLines: List[String] = List[String]()

  private var _intern: ActorRef[Request] = _
  private var _online: Boolean = false

  def intern: ActorRef[Request] = _intern
  def intern_= (ref: ActorRef[Request]): Unit = _intern = ref

  def online: Boolean = _online
  def online_= (status: Boolean): Unit = _online = status

  def start(testName: String): Future[Boolean] = {
    MDC.put("runtime-id", runtimeIdentifier.toString)
    MDC.put("test", testName.split('.').apply(1).capitalize)

    _online = false
    _logLines = _logLines.empty
    _process = Configuration.executable().run(ProcessLogger(line => _logLines = line :: _logLines , line => _logLines.appended(line)))

    Future {
      while (!online) {
        intern ! Session.GetServerStatus
        Thread.sleep(500)
      }

      online
    }
  }

  def stop(): Unit = {
    if (_process != null && _process.isAlive()) {
      _process.destroy()

      Logger("process").info(_logLines.reverse.mkString("\r\n"))
      intern ! Session.ReportStatus

      if (_process.isAlive) {
        throw new Exception("ERROR_SHUTTING_DOWN_PROCESS")
      }
    }
  }

  def spawnClient(client: Client): Client = {
    implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
    implicit val system: ActorSystem[_] = PriceIRC.system

    Await.result(intern.ask[SpawnedClient](replyTo => SpawnClient(client, replyTo)), timeout.duration).client
  }
}

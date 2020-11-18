package de.rubenmaurer.price.core.facade

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.core._
import de.rubenmaurer.price.core.networking.ConnectionHandler
import de.rubenmaurer.price.util.Configuration
import de.rubenmaurer.price.util.Configuration.runtimeIdentifier
import org.slf4j.MDC

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.sys.process.{ProcessLogger, _}

object Session {
  val facade: Session = new Session()

  def apply(suite: ActorRef[Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      def refreshServerStatus(): Behavior[Command] = {
        if (!facade.online) {
          var dummy: ActorRef[String] = context.system.ignoreRef

          try {
            dummy = context.actorOf(ConnectionHandler.apply(context.system.ignoreRef)).toTyped
            facade.online = true
          } catch {
            case _: Throwable => facade.online = false
            Behaviors.stopped
          } finally {
            dummy ! "close"
          }
        }

        Behaviors.same
      }

      facade.intern = context.self

      Behaviors.receive[Command] { (context, message) =>
        message match {
          case Request(command, sender) =>
            command match {
              case SpawnClient(preset) =>
                context.log.debug("SpawnClient")
                preset.link(context.spawn(Client(preset), preset.username))
                Thread.sleep(50)

                sender ! Reply(preset, context.self)
            }

          case Reply(payload, _) =>
            println(payload)

          case SingleTestFinished =>
            context.children.foreach(f => context.stop(f))
            suite ! SingleTestFinished

          case RefreshServerStatus => refreshServerStatus()
        }

        Behaviors.same
      }
    }
  }
}

class Session() {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  private var _process: Process = _
  private var _logLines: List[String] = List[String]()

  private var _intern: ActorRef[Command] = _
  private var _online: Boolean = false

  private var _logger: Logger = _

  def intern: ActorRef[Command] = _intern
  def intern_= (ref: ActorRef[Command]): Unit = _intern = ref

  def online: Boolean = _online
  def online_= (status: Boolean): Unit = _online = status

  def logger: Logger = _logger

  def start(testName: String): Future[Boolean] = {
    MDC.put("runtime-id", runtimeIdentifier.toString)
    MDC.put("test", testName)

    _logger = Logger("test")
    _logLines = _logLines.empty
    _process = Configuration.executable().run(ProcessLogger(line => _logLines = line :: _logLines , line => _logLines.appended(line)))

    Future {
      while (!online) intern ! RefreshServerStatus
      online
    }
  }

  def stop(): Unit = {
    if (_process != null && _process.isAlive()) {
      _process.destroy()

      Logger("process").info(_logLines.reverse.mkString("\r\n"))
      intern ! SingleTestFinished

      if (_process.isAlive) {
        throw new Exception("ERROR_SHUTTING_DOWN_PROCESS")
      }
    }
  }

  def spawnClient(client: Client): Client = {
    implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
    implicit val system: ActorSystem[_] = PriceIRC.system

    Await.result(intern.ask[Reply[Client]](pre => Request(SpawnClient(client), pre)), timeout.duration).payload
  }
}

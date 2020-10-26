package de.rubenmaurer.price.core.facade

import java.io.FileWriter
import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ClassicActorRefOps, TypedActorContextOps}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import de.rubenmaurer.price.core._
import de.rubenmaurer.price.core.networking.ConnectionHandler
import de.rubenmaurer.price.util.Configuration
import org.scalatest.Assertions.fail

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.sys.process.{ProcessLogger, _}

object Session {
  val facade: Session = new Session()

  def apply(suite: ActorRef[Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      def refreshServerStatus(): Behavior[Command] = {
        try {
          context.actorOf(ConnectionHandler.props(context.system.ignoreRef)).toTyped
          facade.online = true
        } catch {
          case _: Throwable => facade.online = false
        }

        Behaviors.same
      }

      facade.intern = context.self

      Behaviors.receive[Command] { (context, message) =>
        message match {
          case SpawnClient(preset) =>
            val intern = context.spawn(Client(preset), preset.username)
            val connectionHandler = context.actorOf(ConnectionHandler.props(intern)).toTyped

            preset.link(intern)
            Behaviors.same

          case SingleTestFinished =>
            context.children.foreach(f => context.stop(f))
            suite ! SingleTestFinished
            Behaviors.same

          case RefreshServerStatus => refreshServerStatus()
        }
      }
    }
  }
}

class Session() {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  private var _process: Process = _
  private var _logLines: List[String] = List[String]()
  private var _writer: FileWriter = _
  private var _intern: ActorRef[Command] = _
  private var _online: Boolean = false

  def intern: ActorRef[Command] = _intern
  def intern_= (ref: ActorRef[Command]): Unit = _intern = ref

  def online: Boolean = _online
  def online_= (status: Boolean): Unit = _online = status

  def start(testName: String): Unit = {
    _logLines = _logLines.empty
    _writer = new FileWriter(Configuration.getLogFile(testName))
    _process = Configuration.executable().run(ProcessLogger(line => _logLines = line :: _logLines , line => _logLines.appended(line)))

    //Thread.sleep(5000) //dafq?!
    Await.ready(Future {
      do {
        //intern ! RefreshServerStatus
      } while (!online)
    }, timeout.duration)
  }

  def stop(): Unit = {
    if (_process != null && _process.isAlive()) {
      _process.destroy()

      _writer.write(_logLines.reverse.mkString("\r\n"))
      _writer.flush()
      _writer.close()

      intern ! SingleTestFinished

      if (_process.isAlive) {
        throw new Exception("ERROR_SHUTTING_DOWN_PROCESS")
      }
    }
  }

  def spawnClient(client: Client): Client = {
    Await.result(Future {
      intern ! SpawnClient(client)
      while (!client.linked) Thread.sleep(50)

      client
    }, timeout.duration)
  }
}

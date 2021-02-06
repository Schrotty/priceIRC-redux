package de.rubenmaurer.price.core.networking

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{Actor, Props}
import akka.io.Tcp.CommandFailed
import akka.io.{IO, Tcp}
import akka.util.ByteString
import de.rubenmaurer.price.core.networking.ConnectionHandler.{Disconnect, Response, Send}
import de.rubenmaurer.price.util.Configuration

object ConnectionHandler {
  sealed trait Request
  final case class Send(message: String, expected: Int, replyTo: ActorRef[Response]) extends Request
  final case object Disconnect extends Request

  sealed trait Response
  final case class Received(payload: String) extends Response
  final case class Success(replyTo: ActorRef[Request]) extends Response
  final case class Failure(failureMessage: CommandFailed) extends Response

  def apply(listener: ActorRef[Response]): Props = Props(new ConnectionHandler(listener))
}

class ConnectionHandler(listener: ActorRef[Response]) extends Actor {

  import Tcp._
  import context.system

  //connect to server
  IO(Tcp) ! Connect(InetSocketAddress.createUnresolved(Configuration.hostname(), Configuration.port()))

  def receive: Receive = {
    case CommandFailed(c: Connect) =>
      listener ! ConnectionHandler.Failure(c.failureMessage)
      context.stop(self)

    case Connected(_, _) =>
      val connection = sender()
      connection ! Register(self)
      listener ! ConnectionHandler.Success(context.self.toTyped)

      context.become {
        case Send(payload, expected, replyTo) => connection ! Write(ByteString(payload.concat("\r\n")))
        case Received(data) => listener ! ConnectionHandler.Received(data.decodeString("US-ASCII"))
        case Disconnect => connection ! Close
        case _: ConnectionClosed => context.stop(self)
      }
  }
}

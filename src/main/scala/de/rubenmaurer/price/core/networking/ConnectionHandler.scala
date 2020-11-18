package de.rubenmaurer.price.core.networking

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import de.rubenmaurer.price.core.networking.ConnectionHandler.{Response, Send}
import de.rubenmaurer.price.util.Configuration

object ConnectionHandler {
  sealed trait Request
  final case class Send(message: String, expected: Int, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  final case class Received(payload: String) extends Response

  def apply(listener: ActorRef[Response]): Props = Props(new ConnectionHandler(listener))
}

class ConnectionHandler(listener: ActorRef[Response]) extends Actor {

  var expectedResponseLines = 0

  import Tcp._
  import context.system

  //connect to server
  IO(Tcp) ! Connect(InetSocketAddress.createUnresolved(Configuration.hostname(), Configuration.port()))

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      context.stop(self)

    case Connected(_, _) =>
      val connection = sender()
      connection ! Register(self)

      context.become {
        case Send(payload, expected, replyTo) =>
          this.expectedResponseLines = expected
          connection ! Write(ByteString(payload))

          if (this.expectedResponseLines == 0) {
            replyTo ! ConnectionHandler.Received("")
          }

        case Received(data) =>
          val message = data.decodeString("US-ASCII")
          val lines = message.split("\r\n")

          if (this.expectedResponseLines <= lines.size) {
            listener ! ConnectionHandler.Received(message)
          }

        case "close" =>
          connection ! Close

        case _: ConnectionClosed =>
          context.stop(self)
      }
  }
}

package de.rubenmaurer.price.core.networking

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import de.rubenmaurer.price.core.{Command, Parse, Reply, Send}
import de.rubenmaurer.price.util.Configuration

object ConnectionHandler {
  def apply(listener: ActorRef[Command]): Props = Props(new ConnectionHandler(listener))
}

class ConnectionHandler(listener: ActorRef[Command]) extends Actor {

  var expectedResponseLines = 0

  import Tcp._
  import context.system

  //connect to server
  IO(Tcp) ! Connect(InetSocketAddress.createUnresolved(Configuration.hostname(), Configuration.port()))

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      context.stop(self)

    case c @ Connected(_, _) =>
      val connection = sender()
      connection ! Register(self)

      context.become {
        case Send(payload, expected) =>
          this.expectedResponseLines = expected
          connection ! Write(ByteString(payload))

          if (this.expectedResponseLines == 0) {
            listener ! Reply("done", context.self.toTyped)
          }

        case Received(data) =>
          val message = data.decodeString("US-ASCII")
          val lines = message.split("\r\n")

          if (this.expectedResponseLines <= lines.size) {
            listener ! Reply("done", context.self.toTyped)
          }

        case "close" =>
          connection ! Close

        case _: ConnectionClosed =>
          context.stop(self)
      }
  }
}

package de.rubenmaurer.price.core.networking

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import de.rubenmaurer.price.core.Command
import de.rubenmaurer.price.util.Configuration

object ConnectionHandler {
  def props(listener: ActorRef[Command]): Props = Props(new ConnectionHandler(listener))
}

class ConnectionHandler(listener: ActorRef[Command]) extends Actor {

  import Tcp._
  import context.system

  //connect to server
  IO(Tcp) ! Connect(InetSocketAddress.createUnresolved(Configuration.hostname(), Configuration.port()))

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      context.stop(self)

    case Connected(remote, local) =>
      //listener ! RegisterListener(self.toTyped)
      val connection = sender()
      connection ! Register(self)

      context.become {
        case data: ByteString =>
          connection ! Write(data)

        case Received(data) =>
          printf(data.decodeString("US-ASCII"))

        case "close" =>
          connection ! Close

        case _: ConnectionClosed =>
          context.stop(self)
      }
  }
}

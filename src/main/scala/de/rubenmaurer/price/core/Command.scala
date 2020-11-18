package de.rubenmaurer.price.core

import akka.actor.typed.ActorRef
import de.rubenmaurer.price.core.facade.Client

trait Command

case class Request[A](command: Command, sender: ActorRef[Reply[A]]) extends Command

case class Reply[A](payload: A, sender: ActorRef[Command]) extends Command
case class SpawnClient(preset: Client) extends Command
case class SpawnedClient(preset: Client) extends Command

case class Send(payload: String, expected: Int = 0) extends Command
case class Debug(message: String) extends Command
case object Done extends Command

case object SpawnConnection extends Command
case object RunSingleTest extends Command
case object RunTestSuite extends Command

case object SingleTestFinished extends Command
case object AllTestsFinished extends Command

case object RefreshServerStatus extends Command

case class ConnectionEstablished(actorRef: ActorRef[_]) extends Command
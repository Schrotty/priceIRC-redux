package de.rubenmaurer.price.core

import akka.actor.typed.ActorRef
import de.rubenmaurer.price.core.facade.Client

trait Command
trait ReplyType

case class Request(command: Command, sender: ActorRef[Reply]) extends Command
case class Reply(ref: ActorRef[Any]) extends Command
case class SpawnClient(preset: Client) extends Command
case class RegisterListener(actorRef: ActorRef[Command]) extends Command

case object SpawnConnection extends Command
case object RunSingleTest extends Command
case object RunTestSuite extends Command

case object SingleTestFinished extends Command
case object AllTestsFinished extends Command

case object RefreshServerStatus extends Command

case class ConnectionEstablished(actorRef: ActorRef[_]) extends Command
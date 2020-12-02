package de.rubenmaurer.price.core.facade

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.antlr.{IRCLexer, IRCParser}
import de.rubenmaurer.price.core.facade.Parser.{Parse, ParseResult}
import de.rubenmaurer.price.core.parser.antlr.{PricefieldErrorListener, PricefieldListener}
import de.rubenmaurer.price.util.IRCCode
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}

import scala.concurrent.Await

object Parser {
  val facade: Parser = new Parser()

  sealed trait Command
  final case class Parse(message: String, parserRule: Int, replyTo: ActorRef[ParseResult]) extends Command
  final case class ParseResult(result: List[String]) extends Command

  def apply(): Behavior[Parse] = Behaviors.setup { context =>
    facade.intern = context.self

    Behaviors.receive[Parse] { (_, request) =>
      val errorListener = new PricefieldErrorListener()
      val parser = new IRCParser(
        new CommonTokenStream(
          new IRCLexer(CharStreams.fromString(request.message))
        )
      )

      val rule = request.parserRule match {
        case IRCParser.RULE_pong => parser.pong()
        case _ => parser.response()
      }

      parser.removeErrorListeners()
      parser.addErrorListener(errorListener)
      rule.enterRule(new PricefieldListener())

      request.replyTo ! ParseResult(errorListener.exceptions)
      Behaviors.same
    }
  }
}

class Parser() {
  private var _intern: ActorRef[Parse] = _
  private implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  private implicit val system: ActorSystem[_] = PriceIRC.system

  def intern: ActorRef[Parse] = _intern
  def intern_= (ref: ActorRef[Parse]): Unit = _intern = ref

  private def parse(message: String, parserRule: Int): ParseResult = {
    if (message.isBlank || message.isEmpty) throw new RuntimeException("Empty parse messages are not allowed!")
    Await.result(intern.ask[ParseResult](replyTo => Parse(message, parserRule, replyTo)), timeout.duration)
  }

  private def isValid(message: String, parserRule: Int): Boolean = {
    parse(message, parserRule).result.isEmpty
  }

  /* === plain log methods === */
  def isPong(client: Client): Boolean = {
    isValid(client.log.startWith("PONG"), IRCParser.RULE_pong)
  }

  def isEmpty(client: Client): Boolean = {
    client.log.plain.isEmpty
  }

  /* === code log methods === */
  def isUnknown(client: Client): Boolean = {
    isValid(client.log.codes.getOrElse(IRCCode.unknown_command, ""), IRCParser.RULE_unknown_command)
  }
}

package de.rubenmaurer.price.core.facade

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

import java.util.concurrent.TimeUnit
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
      val lexer = new IRCLexer(CharStreams.fromString(request.message))

      val parser = new IRCParser(new CommonTokenStream(lexer))
      parser.removeErrorListeners()
      parser.addErrorListener(errorListener)

      val rule = request.parserRule match {
        case IRCParser.RULE_pong => parser.pong()
        case IRCParser.RULE_unknown_command => parser.unknown_command()
        case IRCParser.RULE_motd => parser.motd()

        /* === WELCOME === */
        case IRCParser.RULE_welcome => parser.welcome()
        case IRCParser.RULE_your_host => parser.your_host()
        case IRCParser.RULE_created => parser.created()
        case IRCParser.RULE_my_info => parser.my_info()

        /* === WHOIS === */
        case IRCParser.RULE_who_is_user => parser.who_is_user()
        case IRCParser.RULE_who_is_server => parser.who_is_server()
        case IRCParser.RULE_end_of_who_is => parser.end_of_who_is()

        /* === UTIL ===*/
        case IRCParser.RULE_no_such_nick_channel => parser.no_such_nick_channel()

        case _ => parser.response()
      }

      parser.removeParseListeners()
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
    var result = List[String]()

    try {
      result = parse(message, parserRule).result
      if (result.nonEmpty) {
        throw new RuntimeException(result.head)
      }
    } catch {
      case e: Throwable =>
        Session.logger.error(e.getMessage)
        return false
    }

    result.isEmpty
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
    isValid(client.log.byCode(IRCCode.unknown_command), IRCParser.RULE_unknown_command)
  }

  def isWelcome(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.welcome), IRCParser.RULE_welcome) &&
      isValid(client.log.byCode(IRCCode.your_host), IRCParser.RULE_your_host) &&
      isValid(client.log.byCode(IRCCode.created), IRCParser.RULE_created) &&
      isValid(client.log.byCode(IRCCode.my_info), IRCParser.RULE_my_info)
  }

  def isWhois(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.who_is_user), IRCParser.RULE_who_is_user) &&
      isValid(client.log.byCode(IRCCode.who_is_server), IRCParser.RULE_who_is_server) &&
      isValid(client.log.byCode(IRCCode.end_of_who_is), IRCParser.RULE_end_of_who_is)
  }

  def isNoSuchNick(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.no_such_nick), IRCParser.RULE_no_such_nick_channel)
  }
}

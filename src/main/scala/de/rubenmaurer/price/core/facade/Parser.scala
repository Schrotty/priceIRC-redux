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

        /* === PRIVATE MESSAGES/ NOTICE === */
        case IRCParser.RULE_private_message => parser.private_message()
        case IRCParser.RULE_notice => parser.notice()

        /* === LUSER === */
        case IRCParser.RULE_luser_client => parser.luser_client()
        case IRCParser.RULE_luser_op => parser.luser_op()
        case IRCParser.RULE_luser_unknown => parser.luser_unknown()
        case IRCParser.RULE_luser_channel => parser.luser_channel()
        case IRCParser.RULE_luser_me => parser.luser_me()

        /* === JOIN === */
        case IRCParser.RULE_name_reply => parser.name_reply()
        case IRCParser.RULE_end_of_names => parser.end_of_names()

        /* === WHO === */
        case IRCParser.RULE_who => parser.who()
        case IRCParser.RULE_end_of_who => parser.end_of_who()

        /* === UTIL ===*/
        case IRCParser.RULE_no_such_nick_channel => parser.no_such_nick_channel()
        case IRCParser.RULE_nickname_in_use => parser.nickname_in_use()
        case IRCParser.RULE_quit => parser.quit()
        case IRCParser.RULE_topic => parser.topic()

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
    if (message.isBlank || message.isEmpty)
      throw new RuntimeException(s"Empty parse messages are not allowed!")

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

  def isQuit(client: Client): Boolean = {
    isValid(client.log.last, IRCParser.RULE_quit)
  }

  def isPrivateMessage(client: Client): Boolean = {
    isValid(client.log.last, IRCParser.RULE_private_message)
  }

  def isNotice(client: Client): Boolean = {
    isValid(client.log.last, IRCParser.RULE_notice)
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

  def isNicknameInUse(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.nickname_in_use), IRCParser.RULE_nickname_in_use)
  }

  def isLUser(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.luser_client), IRCParser.RULE_luser_client) &&
      isValid(client.log.byCode(IRCCode.luser_op), IRCParser.RULE_luser_op) &&
      isValid(client.log.byCode(IRCCode.luser_unknown), IRCParser.RULE_luser_unknown) &&
      isValid(client.log.byCode(IRCCode.luser_channel), IRCParser.RULE_luser_channel) &&
      isValid(client.log.byCode(IRCCode.luser_me), IRCParser.RULE_luser_me)
  }

  def isJoin(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.name_reply), IRCParser.RULE_name_reply) &&
      isValid(client.log.byCode(IRCCode.end_of_names), IRCParser.RULE_end_of_names)
  }

  def isWho(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.who_reply), IRCParser.RULE_who) &&
      isValid(client.log.byCode(IRCCode.end_of_who), IRCParser.RULE_end_of_who)
  }

  def isTopic(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.topic), IRCParser.RULE_topic) || isValid(client.log.find("TOPIC"), IRCParser.RULE_topic)
  }

  def isNoTopicSet(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.no_topic), IRCParser.RULE_no_topic)
  }

  def isNotOnChannel(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.not_on_channel), IRCParser.RULE_not_on_channel)
  }

  def isPart(client: Client): Boolean = {
    isValid(client.log.find("PART"), IRCParser.RULE_part)
  }

  def isNoSuchChannel(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.no_such_channel), IRCParser.RULE_no_such_nick_channel) ||
      isValid(client.log.byCode(IRCCode.no_such_nick), IRCParser.RULE_no_such_nick_channel)

  }

  def isNick(client: Client): Boolean = {
    isValid(client.log.find("NICK"), IRCParser.RULE_nick_reply)
  }

  def isList(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.list), IRCParser.RULE_list) && isValid(client.log.byCode(IRCCode.list_end), IRCParser.RULE_listend)
  }

  def isCannotSendToChannel(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.cannot_send_to_channel), IRCParser.RULE_cannot_send_to_channel)

  }
}

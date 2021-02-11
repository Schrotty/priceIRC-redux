package de.rubenmaurer.price.core.facade

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import de.rubenmaurer.price.PriceIRC
import de.rubenmaurer.price.antlr.{IRCLexer, IRCParser}
import de.rubenmaurer.price.core.facade.Parser.{Parse, ParseData, ParseResult}
import de.rubenmaurer.price.core.parser.antlr.{PricefieldErrorListener, PricefieldListener}
import de.rubenmaurer.price.util.{Channel, IRCCode, Target}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await

object Parser {
  val facade: Parser = new Parser()

  sealed trait Command
  final case class Parse(message: String, parserRule: Int, replyTo: ActorRef[ParseResult], data: ParseData) extends Command
  final case class ParseResult(errors: List[String]) extends Command

  final case class ParseData(code: Int = 0, user: String = "", nick: String = "", fullname: String = "",
                             message: String = "", users: Int = 0, channels: Int = 0, unknown: Int = 0, clients: Int = 0,
                             command: String = "", channel: String = "", names: Seq[String] = List(), target: String = "")

  def apply(): Behavior[Parse] = Behaviors.setup { context =>
    facade.intern = context.self

    Behaviors.receive[Parse] { (_, request) =>
      val parserListener = new PricefieldListener(request.data)
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
      rule.enterRule(parserListener)

      request.replyTo ! ParseResult(parserListener.errors ++ errorListener.exceptions)
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

  private def parse(message: String, parserRule: Int, parseData: ParseData): ParseResult = {
    if (message.isBlank || message.isEmpty)
      throw new RuntimeException(s"Empty parse messages are not allowed!")

    Await.result(intern.ask[ParseResult](replyTo => Parse(message, parserRule, replyTo, parseData)), timeout.duration)
  }

  private def isValid(message: String, parserRule: Int, parseData: ParseData = ParseData()): Boolean = {
    try {
      val result = parse(message, parserRule, parseData)
      if (result.errors.nonEmpty) {
        throw new RuntimeException(result.errors.head)
      }
    } catch {
      case e: Throwable =>
        Session.logger.error(e.getMessage)
        return false
    }

    true
  }

  /* === PLAIN LOG METHODS === */
  def isEmpty(client: Client): Boolean = {
    client.log.plain.isEmpty
  }

  /* === NON CODE METHODS */
  def isPong(client: Client): Boolean = {
    isValid(client.log.startWith("PONG"), IRCParser.RULE_pong)
  }

  def isNick(client: Client): Boolean = {
    isValid(client.log.find("NICK"), IRCParser.RULE_nick_reply, ParseData(nick = client.nickname))
  }

  def isQuit(client: Client, quitMessage: String = "Client Quit"): Boolean = {
    isValid(client.log.last, IRCParser.RULE_quit, ParseData(message = quitMessage))
  }

  def isPrivateMessage(client: Client, message: String): Boolean = {
    isValid(client.log.last, IRCParser.RULE_private_message, ParseData(message = message))
  }

  def isNotice(client: Client, message: String): Boolean = {
    isValid(client.log.last, IRCParser.RULE_notice, ParseData(message = message))
  }

  def isPart(client: Client, channel: Channel, message: String = ""): Boolean = {
    isValid(client.log.find("PART"), IRCParser.RULE_part, ParseData(channel = channel.toString, message = message))
  }

  /* === CODE LOG METHODS === */
  def isUnknown(client: Client, command: String): Boolean = {
    isValid(client.log.byCode(IRCCode.unknown_command), IRCParser.RULE_unknown_command, ParseData(command = command))
  }

  def isWelcome(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.welcome), IRCParser.RULE_welcome, ParseData(nick = client.nickname, user = client.username)) &&
      isValid(client.log.byCode(IRCCode.your_host), IRCParser.RULE_your_host) &&
      isValid(client.log.byCode(IRCCode.created), IRCParser.RULE_created) &&
      isValid(client.log.byCode(IRCCode.my_info), IRCParser.RULE_my_info)
  }

  def isMessageOfTheDay(client: Client, message: String): Boolean = {
    isValid(client.log.byCode(IRCCode.motd_start), IRCParser.RULE_motd_start) &&
      isValid(client.log.byCode(IRCCode.motd), IRCParser.RULE_motd, ParseData(message = message)) &&
      isValid(client.log.byCode(IRCCode.end_of_motd), IRCParser.RULE_end_of_motd)
  }

  def isNoMessageOfTheDay(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.no_motd), IRCParser.RULE_no_motd)
  }

  def isWhois(client: Client, target: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.who_is_user), IRCParser.RULE_who_is_user, ParseData(nick = target.nickname, user = target.username, fullname = target.fullName)) &&
      isValid(client.log.byCode(IRCCode.who_is_server), IRCParser.RULE_who_is_server, ParseData(nick = target.nickname)) &&
      isValid(client.log.byCode(IRCCode.end_of_who_is), IRCParser.RULE_end_of_who_is, ParseData(nick = target.nickname))
  }

  def isNoSuchNick(client: Client, target: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.no_such_nick), IRCParser.RULE_no_such_nick_channel, ParseData(target = target.nickname))
  }

  def isNicknameInUse(client: Client): Boolean = {
    isValid(client.log.byCode(IRCCode.nickname_in_use), IRCParser.RULE_nickname_in_use)
  }

  def isLUser(client: Client, clients: Int = 1, channels: Int = 0, unknown: Int = 0, users: Int = 1): Boolean = {
    isValid(client.log.byCode(IRCCode.luser_client), IRCParser.RULE_luser_client, ParseData(clients = clients)) &&
      isValid(client.log.byCode(IRCCode.luser_op), IRCParser.RULE_luser_op) &&
      isValid(client.log.byCode(IRCCode.luser_unknown), IRCParser.RULE_luser_unknown, ParseData(unknown = unknown)) &&
      isValid(client.log.byCode(IRCCode.luser_channel), IRCParser.RULE_luser_channel, ParseData(channels = channels)) &&
      isValid(client.log.byCode(IRCCode.luser_me), IRCParser.RULE_luser_me, ParseData(users = users))
  }

  def isJoin(client: Client, channel: Channel, names: Client*): Boolean = {
    isValid(client.log.byCode(IRCCode.name_reply), IRCParser.RULE_name_reply, ParseData(channel = channel.toString, names = names.map(f => f.nickname))) &&
      isValid(client.log.byCode(IRCCode.end_of_names), IRCParser.RULE_end_of_names, ParseData(channel = channel.toString))
  }

  def isWho(client: Client, channel: Channel, user: Client*): Boolean = {
    user.forall(f => isValid(client.log.byCodeAnd(IRCCode.who_reply, f.fullName), IRCParser.RULE_who, ParseData(channel = channel.toString, nick = f.nickname, user = f.username, fullname = f.fullName))) &&
      isValid(client.log.byCode(IRCCode.end_of_who), IRCParser.RULE_end_of_who, ParseData(channel = channel.toString))
  }

  def isTopic(client: Client, channel: Channel, message: String = ""): Boolean = {
    isValid(client.log.byCode(IRCCode.topic), IRCParser.RULE_topic, ParseData(channel = channel.toString, message = message)) ||
      isValid(client.log.find("TOPIC"), IRCParser.RULE_topic, ParseData(channel = channel.toString, message = message))
  }

  def isNoTopicSet(client: Client, channel: Channel): Boolean = {
    isValid(client.log.byCode(IRCCode.no_topic), IRCParser.RULE_no_topic, ParseData(channel = channel.toString))
  }

  def isNotOnChannel(client: Client, channel: Channel): Boolean = {
    isValid(client.log.byCode(IRCCode.not_on_channel), IRCParser.RULE_not_on_channel, ParseData(channel = channel.toString))
  }

  def isNoSuchChannel(client: Client, target: Target): Boolean = {
    isValid(client.log.byCode(IRCCode.no_such_channel), IRCParser.RULE_no_such_nick_channel, ParseData(target = target.toString)) ||
      isValid(client.log.byCode(IRCCode.no_such_nick), IRCParser.RULE_no_such_nick_channel, ParseData(target = target.toString))
  }

  def isList(client: Client, channel: Channel, clients: Int = 1, topic: String = ""): Boolean = {
    isValid(client.log.byCode(IRCCode.list), IRCParser.RULE_list, ParseData(channel = channel.toString, clients = clients, message = topic)) &&
      isValid(client.log.byCode(IRCCode.list_end), IRCParser.RULE_listend)
  }

  def isCannotSendToChannel(client: Client, channel: Channel): Boolean = {
    isValid(client.log.byCode(IRCCode.cannot_send_to_channel), IRCParser.RULE_cannot_send_to_channel, ParseData(channel = channel.toString))
  }
}

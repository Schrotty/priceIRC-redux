package de.rubenmaurer.price.core.parser.antlr

import de.rubenmaurer.price.antlr4.{IRCBaseListener, IRCParser}
import de.rubenmaurer.price.core.facade.Parser.ParseData
import de.rubenmaurer.price.util.TemplateManager

class PricefieldListener(parseData: ParseData) extends IRCBaseListener {

  var errors: List[String] = List()

  override def enterServer_response_short(ctx: IRCParser.Server_response_shortContext): Unit = {
    stringCheck(ctx.nick().getText, parseData.nick, ctx.getText)
    stringCheck(ctx.user().getText, parseData.user, ctx.getText)
  }

  override def enterQuit(ctx: IRCParser.QuitContext): Unit = {
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  override def enterPrivate_message(ctx: IRCParser.Private_messageContext): Unit = {
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  override def enterNotice(ctx: IRCParser.NoticeContext): Unit = {
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  override def enterUnknown_command(ctx: IRCParser.Unknown_commandContext): Unit = {
    stringCheck(ctx.command().getText, parseData.command, ctx.getText)
  }

  override def enterMotd(ctx: IRCParser.MotdContext): Unit = {
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  override def enterNick_reply(ctx: IRCParser.Nick_replyContext): Unit = {
    stringCheck(ctx.nick().getText, parseData.message, ctx.getText)
  }

  /* === WHO === */
  override def enterWho(ctx: IRCParser.WhoContext): Unit = {
    if (parseData.channel != "*") stringCheck(ctx.target().getText, parseData.channel, ctx.getText)

    stringCheck(ctx.nick().getText, parseData.nick, ctx.getText)
    stringCheck(ctx.user().getText, parseData.user, ctx.getText)
    stringCheck(ctx.fullname().getText, parseData.fullname, ctx.getText)
  }

  override def enterEnd_of_who(ctx: IRCParser.End_of_whoContext): Unit = {
    if (parseData.channel != "*") stringCheck(ctx.target().getText, parseData.channel, ctx.getText)
  }

  /* === TOPIC === */

  override def enterTopic(ctx: IRCParser.TopicContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  override def enterNo_topic(ctx: IRCParser.No_topicContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
  }

  /* === CHANNEL/ NICK ERROR ===*/
  override def enterNo_such_nick_channel(ctx: IRCParser.No_such_nick_channelContext): Unit = {
    stringCheck(ctx.target().getText, parseData.target, ctx.getText)
  }

  override def enterCannot_send_to_channel(ctx: IRCParser.Cannot_send_to_channelContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
  }

  /* === PART === */
  override def enterPart(ctx: IRCParser.PartContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  /* === LIST === */
  override def enterList(ctx: IRCParser.ListContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
    integerCheck(ctx.INTEGER().getText, parseData.clients, ctx.getText)
    stringCheck(ctx.message().getText, parseData.message, ctx.getText)
  }

  /* === NAME LIST ===*/
  override def enterName_reply(ctx: IRCParser.Name_replyContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
    stringCheck(ctx.nicknames().getText, s"@${parseData.names.mkString(" ")}", ctx.getText)
  }

  override def enterEnd_of_names(ctx: IRCParser.End_of_namesContext): Unit = {
    stringCheck(ctx.channel().getText, parseData.channel, ctx.getText)
  }

  /* === WHOIS === */
  override def enterWho_is_user(ctx: IRCParser.Who_is_userContext): Unit = {
    stringCheck(ctx.nick().getText, parseData.nick, ctx.getText)
    stringCheck(ctx.user().getText, parseData.user, ctx.getText)
    stringCheck(ctx.fullname().getText, parseData.fullname, ctx.getText)
  }

  override def enterWho_is_server(ctx: IRCParser.Who_is_serverContext): Unit = {
    stringCheck(ctx.nick().getText, parseData.nick, ctx.getText)
  }

  override def enterEnd_of_who_is(ctx: IRCParser.End_of_who_isContext): Unit = {
    stringCheck(ctx.nick().getText, parseData.nick, ctx.getText)
  }

  /* === LUSER === */
  override def enterLuser_client(ctx: IRCParser.Luser_clientContext): Unit = {
    integerCheck(ctx.INTEGER(0).getText, parseData.clients, ctx.getText)
  }

  override def enterLuser_unknown(ctx: IRCParser.Luser_unknownContext): Unit = {
    integerCheck(ctx.INTEGER.getText, parseData.unknown, ctx.getText)
  }

  override def enterLuser_channel(ctx: IRCParser.Luser_channelContext): Unit = {
    integerCheck(ctx.INTEGER.getText, parseData.channels, ctx.getText)
  }

  override def enterLuser_me(ctx: IRCParser.Luser_meContext): Unit = {
    integerCheck(ctx.INTEGER(0).getText, parseData.users, ctx.getText)
  }

  /* === VALUE CHECKS === */
  def stringCheck(expected: String, actual: String, line: String): Unit = {
    if (!expected.equals(actual)) {
      errors = TemplateManager.getCompareFailure("String", expected, actual, line) :: errors
    }
  }

  def integerCheck(expected: String, actual: Int, line: String): Unit = integerCheck(expected.toInt, actual, line)

  def integerCheck(expected: Int, actual: Int, line: String): Unit = {
    if (expected != actual) {
      errors = TemplateManager.getCompareFailure("Integer", expected, actual, line) :: errors
    }
  }
}
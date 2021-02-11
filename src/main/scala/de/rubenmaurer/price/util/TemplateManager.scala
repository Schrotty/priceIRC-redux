package de.rubenmaurer.price.util

import de.rubenmaurer.price.test.TestIndex
import org.stringtemplate.v4.{ST, STGroupFile}

object TemplateManager {
  val empty = ""
  private val _templates = new STGroupFile("templates/system.stg")

  private def getTemplate(key: String): String = {
    _templates.getInstanceOf(key).render()
  }

  private def getRawTemplate(key: String): ST = {
    _templates.getInstanceOf(key)
  }

  def getStartupMessage: String = getTemplate("startup")
  def getVersionString: String = getRawTemplate("version").add("v", BuildInfo.version).render()
  def getRuntimeID: String = getRawTemplate("runtime").add("id", Configuration.runtimeIdentifier).render()
  def getTests: String = getRawTemplate("tests").add("ts", TestIndex.getAll("ALL").mkString(",")).render()
  def getConnectionFailure(address: String): String = getRawTemplate("connectionFailure").add("address", address).render()
  def getCompareFailure(typ: String, expected: Any, actual: Any, line: String): String = getRawTemplate("compareFailure")
    .add("type", typ).add("expected", expected).add("actual", actual).add("line", line).render()

  /* === IRC REQUESTS ===*/
  def getNick(nickname: String): String = getRawTemplate("nick").add("nickname", nickname).render()
  def getUser(username: String, fullname: String): String = getRawTemplate("user").add("username", username)
    .add("fullname", fullname).render()

  def whois(nickname: String): String = getRawTemplate("whois").add("nickname", nickname).render()
  def getQuit(message: String): String = getRawTemplate("quit").add("message", message).render()
  def getPrivateMessage(nickname: String, message: String): String = getRawTemplate("privateMessage")
    .add("nickname", nickname).add("message", message).render()

  def getNotice(nickname: String, message: String): String = getRawTemplate("notice")
    .add("nickname", nickname).add("message", message).render()

  def join(channel: String): String = getRawTemplate("join").add("channel", channel).render()
  def who(channel: String): String = getRawTemplate("who").add("channel", channel).render()
  def getTopic(channel: String): String = getRawTemplate("get_topic").add("channel", channel).render()
  def setTopic(channel: String, topic: String): String = getRawTemplate("set_topic").add("channel", channel)
    .add("topic", topic).render()

  def part(channel: String, message: String): String = getRawTemplate("part").add("channel", channel).add("message", message).render()
  def list(channel: String): String = getRawTemplate("list").add("channel", channel).render()
}

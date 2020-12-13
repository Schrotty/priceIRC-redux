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

  /* === IRC REQUESTS ===*/
  def getNick(nickname: String): String = getRawTemplate("nick").add("nickname", nickname).render()
  def getUser(username: String, fullname: String): String = getRawTemplate("user").add("username", username)
    .add("fullname", fullname).render()

  def whois(nickname: String): String = getRawTemplate("whois").add("nickname", nickname).render()
}

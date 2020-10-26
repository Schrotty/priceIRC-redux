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
}

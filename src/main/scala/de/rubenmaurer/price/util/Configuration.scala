package de.rubenmaurer.price.util

import com.typesafe.config.ConfigFactory

object Configuration {
  val runtimeIdentifier: Number = {
    val uid = java.util.UUID.randomUUID().hashCode()
    if (uid < 0) uid.unary_- else uid
  }

  private val configFactory = ConfigFactory.load()

  def executable(): String = configFactory.getString("price.executable")
  def hostname(): String = configFactory.getString("price.hostname")
  def port(): Int = configFactory.getInt("price.port")
  def tests():String = configFactory.getString("price.tests")
  def logs(): String = configFactory.getString("price.logs")
  def debug(): Boolean = configFactory.getBoolean("price.debug")
}

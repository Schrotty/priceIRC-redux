package de.rubenmaurer.price.util

import java.io.{File, IOException, PrintStream}

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

  def getLogFile(filename: String): File = new File(String.format("%s\\%s\\%s.txt", logs(), runtimeIdentifier, filename))

  def initialSetup(): Unit = {
    val logDir = new File(logs())
    if (!logDir.exists()) {
      if (!logDir.mkdir()) {
        throw new IOException("Unable to create logs folder!")
      }
    }

    val runtimeDirectory = new File(s"${logs()}\\$runtimeIdentifier")
    if (!runtimeDirectory.exists()) {
      if (!runtimeDirectory.mkdir()) {
        throw new IOException("unable ro create runtime folder!")
      }
    }
  }
}

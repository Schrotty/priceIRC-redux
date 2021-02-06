package de.rubenmaurer.price.util

object Channel {
  def ALL: Channel = new Channel("*")
  def BLACKWELL: Channel = new Channel("blackwell")
  def BLACKWELL_ART: Channel = new Channel("blackwell-art")
  def BLACKWELL_SCIENCE: Channel = new Channel("blackwell-science")
  def DINER: Channel = new Channel("Blue-Whale-Diner")
}

class Channel(val name: String) {
  override def toString: String = s"#$name"
}
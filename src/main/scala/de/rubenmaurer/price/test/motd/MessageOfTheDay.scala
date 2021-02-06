package de.rubenmaurer.price.test.motd

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite
import de.rubenmaurer.price.util.Channel

import java.io.{File, PrintWriter}

class MessageOfTheDay(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("list user") {
    assert(parser.isLUser(session.spawnClient(Client.CHLOE).authenticate()))
  }

  test("list multiple user") {
    assert(parser.isLUser(session.spawnClient(Client.KATE).authenticate()))
    assert(parser.isLUser(session.spawnClient(Client.CHLOE).authenticate()))
  }

  test("list multiple user with unregistered") {
    session.spawnClient(Client.CHLOE)
    assert(parser.isLUser(session.spawnClient(Client.MAX).authenticate()))
  }

  test("lusers") {
    assert(parser.isLUser(session.spawnClient(Client.RACHEl).authenticate().lusers()))
  }

  test("lusers for multiple users") {
    session.spawnClient(Client.CHLOE).authenticate()
    session.spawnClient(Client.MAX).authenticate()
    val rachel: Client = session.spawnClient(Client.RACHEl).authenticate()

    assert(parser.isLUser(rachel.lusers()))
  }

  test("lusers with channels") {
    assert(parser.isLUser(session.spawnClient(Client.KATE).authenticate().join(Channel.BLACKWELL)))
  }

  test("lusers mixed") {
    session.spawnClient(Client.KATE).authenticate().join(Channel.DINER)
    session.spawnClient(Client.RACHEl).authenticate().join(Channel.DINER)
    session.spawnClient(Client.MAX).authenticate().join(Channel.BLACKWELL_ART)

    assert(parser.isLUser(session.spawnClient(Client.CHLOE).authenticate().lusers()))
  }

  test("mesage of the day") {
    new PrintWriter("motd.txt") { write("Hello there!"); close() }

    assert(parser.isWelcome(session.spawnClient(Client.KATE).authenticate()))
    new File("motd.txt").delete()
  }
}

package de.rubenmaurer.price.test.channel

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite
import de.rubenmaurer.price.util.Channel

class JoinChannel(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("join channel") {
    val max: Client = session.spawnClient(Client.MAX).authenticate()

    assert(parser.isJoin(max.join(Channel.BLACKWELL)))
  }

  test("join already joined channel") {
    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate().join(Channel.BLACKWELL)
    chloe.log.clearPlain()

    assert(parser.isEmpty(chloe.join(Channel.BLACKWELL)))
  }

  test("multiple joins") {
    val kate: Client = session.spawnClient(Client.KATE).authenticate().join(Channel.DINER)
    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate().join(Channel.DINER)
    val max: Client = session.spawnClient(Client.MAX).authenticate().join(Channel.DINER)

    assert(parser.isJoin(kate) && parser.isJoin(chloe) && parser.isJoin(max))
  }
}

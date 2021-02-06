package de.rubenmaurer.price.test.channel

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite
import de.rubenmaurer.price.util.Channel

class ListChannel(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("list channel without topic") {
    val channel: Channel = Channel.BLACKWELL_SCIENCE

    session.spawnClient(Client.KATE).authenticate().join(channel)
    session.spawnClient(Client.MAX).authenticate().join(channel)
    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate().join(channel).list(channel)

    assert(parser.isList(chloe))
  }

  test("list channel with topic") {
    val channel: Channel = Channel.DINER

    session.spawnClient(Client.RACHEl).authenticate().join(channel)
    session.spawnClient(Client.CHLOE).authenticate().join(channel).topic(channel, "It is a Diner!")
    val chloe: Client = session.spawnClient(Client.KATE).authenticate().join(channel).list(channel)

    assert(parser.isList(chloe))
  }
}

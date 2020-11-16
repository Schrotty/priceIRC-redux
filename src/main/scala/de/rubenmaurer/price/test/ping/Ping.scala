package de.rubenmaurer.price.test.ping

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite

class Ping(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("test-ping") {
    val chloe: Client = session.spawnClient(Client.CHLOE)
    chloe.send("PING", 1)

    assert(parser.isPong(chloe.log.startWith("PONG")))
  }

  test("alt-spawn-multi") {
    val chloe: Client = session.spawnClient(Client.CHLOE)
    val kate: Client = session.spawnClient(Client.KATE)

    chloe.send("PING", 1)
    kate.send("PING", 1)

    assert(parser.isPong(chloe.log.startWith("PONG")))
    assert(parser.isPong(kate.log.startWith("PONG")))
  }
}
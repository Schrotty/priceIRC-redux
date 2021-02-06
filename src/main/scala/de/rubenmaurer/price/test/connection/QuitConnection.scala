package de.rubenmaurer.price.test.connection

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite

class QuitConnection(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("quit with message") {
    assert(parser.isQuit(session.spawnClient(Client.RACHEl).authenticate().quit("Bye")))
  }

  test("plain quit") {
    assert(parser.isQuit(session.spawnClient(Client.KATE).authenticate().quit()))
  }

  test("multi-user quit") {
    val max: Client = session.spawnClient(Client.MAX).authenticate().quit("Goodbye!")
    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate().quit("See ya!")

    assert(parser.isQuit(max))
    assert(parser.isQuit(chloe))
  }
}

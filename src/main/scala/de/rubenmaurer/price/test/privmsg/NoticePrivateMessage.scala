package de.rubenmaurer.price.test.privmsg

import de.rubenmaurer.price.core.facade.{Client, Parser, Session}
import de.rubenmaurer.price.test.BaseTestSuite

class NoticePrivateMessage(session: Session, parser: Parser, testName: String) extends BaseTestSuite(session, parser, testName) {
  test("private message with two clients") {
    val message: String = "I love you!"
    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate()
    val rachel: Client = session.spawnClient(Client.RACHEl).authenticate()

    rachel.await(() => chloe.privateMessage(rachel, message))
    assert(parser.isPrivateMessage(rachel))
  }

  test("private message with four clients") {
    val message = "Hello there!"

    val chloe: Client = session.spawnClient(Client.CHLOE).authenticate()
    val max: Client = session.spawnClient(Client.MAX).authenticate()
    val rachel: Client = session.spawnClient(Client.RACHEl).authenticate()
    val kate: Client = session.spawnClient(Client.KATE).authenticate()

    chloe.await(() => kate.privateMessage(chloe, message))
    assert(parser.isPrivateMessage(chloe))

    max.await(() => chloe.privateMessage(max, message))
    assert(parser.isPrivateMessage(max))

    rachel.await(() => max.privateMessage(rachel, message))
    assert(parser.isPrivateMessage(rachel))

    kate.await(() => rachel.privateMessage(kate, message))
    assert(parser.isPrivateMessage(kate))
  }

  test("simple notice") {
    val message: String = "Oh Hi there!"
    val max: Client = session.spawnClient(Client.MAX).authenticate()
    val kate: Client = session.spawnClient(Client.KATE).authenticate()

    kate.await(() => max.notice(kate, message))
    assert(parser.isNotice(kate))
  }

  test("notice not existing user") {
    val rachel: Client = session.spawnClient(Client.RACHEl).authenticate()
    val max: Client = session.spawnClient(Client.MAX)

    rachel.notice(max, "You there?")
    assert(parser.isEmpty(rachel))
  }
}

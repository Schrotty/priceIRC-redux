package de.rubenmaurer.price.test

import de.rubenmaurer.price.core.facade.{Client, Session}

class UnitTest(session: Session, testName: String) extends BaseTestSuite(session, testName) {
  test("alt-spawn") {
    val chloe: Client = session.spawnClient(Client.CHLOE)

    assert(chloe.nickname == "chloe")
  }

  test("alt-spawn-multi") {
    val chloe: Client = session.spawnClient(Client.CHLOE)
    val kate: Client = session.spawnClient(Client.KATE)
    val max: Client = session.spawnClient(Client.MAX)

    assert(chloe.nickname == "chloe")
    assert(kate.nickname == "kate")
    assert(max.nickname == "max")
  }
}
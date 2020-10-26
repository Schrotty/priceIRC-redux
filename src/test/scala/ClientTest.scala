import de.rubenmaurer.price.core.facade.Client
import org.scalatest.funsuite.AnyFunSuite

class ClientTest extends AnyFunSuite {
  test("chloes username ist 'elisabeth'") {
    assert(Client.CHLOE.username == "elisabeth")
  }
}

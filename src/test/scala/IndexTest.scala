import de.rubenmaurer.price.test.TestIndex
import org.scalatest.funsuite.AnyFunSuite

class IndexTest extends AnyFunSuite {
  test("wat ever") {
    assert(TestIndex.getAll("math").contains("MathSuite"))
  }
}

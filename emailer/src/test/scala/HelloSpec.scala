import org.scalatest._
import flatspec._
import matchers._

class HelloSpec extends AnyFlatSpec with should.Matchers {
  "Hello" should "have tests" in {
    true should === (true)
  }
}

package fpinscala.testing

import org.junit.runner.RunWith
import org.scalacheck.{Gen => SCGen}
import org.scalacheck.{Prop => SCProp}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks
import fpinscala.state.RNG
import org.scalatest.BeforeAndAfterEach

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GenSpec extends FlatSpec with PropertyChecks with BeforeAndAfterEach {

  var rng: RNG = _

  implicit class TestGenOps[A](gen: Gen[A]) {
    def get = {
      val (a, nextRng) = gen.sample.run(rng)
      rng = nextRng
      a
    }
  }

  override def beforeEach = {
    rng = RNG.Simple(0)
  }

  behavior of "8.1 List.sum"

  it should "obey some laws" in {
    val ints = SCGen.choose(0, 100)
    val intList = SCGen.listOf(ints)
    val prop =
      SCProp.forAll(intList) {l => l.sum == l.reverse.sum} &&
      SCProp.forAll(ints, ints) {(n, i) => List.fill(n)(i).sum == n * i}
    prop.check
  }

  behavior of "8.2 List.max"

  it should "obey some laws" in {
    val ints = SCGen.choose(0, 100)
    val intList = SCGen.listOf1(ints)
    val prop =
      SCProp.forAll(intList) {l => l.max == l.reverse.max} &&
      SCProp.forAll(ints map(_ + 1), ints) {(n, i) => List.fill(n)(i).max == i}
    prop.check
  }

  behavior of "8.3 Prop.&&"

  it should "work" in {
    def asProp(b: Boolean): Prop0 = new Prop0 {override def check = b}
    def testAnd[A,B](check1: Boolean, check2: Boolean, expected: Boolean) =
      assertResult(expected)((asProp(check1) && asProp(check2)).check)

    val tests = Table(
      ("prop1.check", "prop2.check", "(prop1 && prop2).check"),
      (true, true, true),
      (true, false, false),
      (false, true, false),
      (false, false, false)
    )
    forAll(tests)(testAnd)
  }

  behavior of "8.4 Gen.choose"

  it should "should stay within specified range" in {
    val startInts = SCGen.choose(-100, 100)
    forAll(startInts label "start") { start =>
      forAll(SCGen.choose(start + 1, start + 101) label "stopExclusive") { stopExclusive =>
        val i = Gen.choose(start, stopExclusive).get
//        println(s"start=$start,stopExclusive=$stopExclusive,i=$i")
        assert(i >= start && i < stopExclusive)//, s"$i >= $start && $i < $stopExclusive")
      }
    }
  }

  behavior of "8.5.1 Gen.unit"

  it should "always return the same object" in {
    forAll("any") { a: Int =>
      assertResult(a)(Gen.unit(a).get)
    }
  }

  behavior of "8.5.2 Gen.boolean"

  it should "have an equal distribution" in {
    val trues = Seq.fill(100)(Gen.boolean.get).filter(x => x)
    assert(math.abs(trues.size - 50) <= 10)
  }

  behavior of "8.5.3 Gen.listOfN"

  it should "generate a list of n elements" in {
    forAll(SCGen.chooseNum(0, 100) label "n") { n =>
      assert(Gen.listOfN(n, Gen.unit(0)).get.size == n)
    }
  }
}

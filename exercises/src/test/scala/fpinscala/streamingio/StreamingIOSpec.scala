package fpinscala.streamingio

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks
import SimpleStreamTransducers.{Process => SSTProcess}
import org.scalacheck.Gen

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StreamingIOSpec extends FlatSpec with PropertyChecks {

  private def between0AndN(n: Int) = Gen.chooseNum(0, n) label "n"

  behavior of "15.1.1 Process.take"
  it should "work" in {
    forAll("l") { l: List[Int] =>
      forAll (between0AndN(l.size)) { n: Int =>
        val result = SSTProcess.take(n)(l.toStream).toList
        assert(result == l.take(n))
      }
    }
  }

  behavior of "15.1.2 Process.drop"
  it should "work" in {
    forAll("l") { l: List[Int] =>
      forAll (between0AndN(l.size)) { n: Int =>
        val result = SSTProcess.drop(n)(l.toStream).toList
        assert(result == l.drop(n))
      }
    }
  }
}
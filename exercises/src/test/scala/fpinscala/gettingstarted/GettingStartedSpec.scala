package fpinscala.gettingstarted

import scala.Array.canBuildFrom
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.Sorting

import org.junit.runner.RunWith
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

import MyModule.fib
import PolymorphicFunctions.compose
import PolymorphicFunctions.curry
import PolymorphicFunctions.isSorted
import PolymorphicFunctions.partial1
import PolymorphicFunctions.uncurry

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GettingStartedSpec extends FlatSpec with PropertyChecks {

  behavior of "2.1 fib"

  it should "work" in {
    val tests = Table(
      ("n", "fib(n)"),
      (0, 0), (1, 1), (2, 1), (3, 2), (4, 3), (5, 5), (6, 8), (7, 13))
    forAll(tests) { (x: Int, expected: Int) =>
      assert(fib(x) == expected)
    }
  }

  it should "be the sum of the previous two fibs" in {
    forAll(Gen.chooseNum(2, 100) :| "n") { n: Int =>
      assert(fib(n - 1) + fib(n - 2) == fib(n))
    }
  }

  behavior of "2.2 isSorted"

  // make use of Scala's built-in Orderings
  import scala.math.Ordering.Implicits._
  def gt[A: Ordering](x: A, y: A) = x >= y

  it should "work" in {

    def testIsSorted[A: Ordering](gt: (A, A) => Boolean)(x: Array[A], expected: Boolean): Unit = {
      assert(isSorted(x, gt) == expected)
    }

    val testsInt = Table(
      ("as", "expected"),
      (Array[Int](), true),
      (Array(0), true),
      (Array(0, 0), true),
      (Array(0, 1), true),
      (Array(0, 1, 2), true),
      (Array(0, 2, 1), false))
    forAll(testsInt)(testIsSorted(gt[Int]))

    val testsString = Table(
      ("as", "y"),
      (Array[String](), true),
      (Array("0"), true),
      (Array("0", "0"), true),
      (Array("0", "1"), true),
      (Array("0", "1", "2"), true),
      (Array("0", "2", "1"), false))
    forAll(testsString)(testIsSorted(gt[String]))
  }

  it should "work for random arrays" in {
    forAll("array") { as: Array[Int] =>
      def toSorted = { val sortedArray = as.clone; Sorting.quickSort(sortedArray); sortedArray }
      val sortedArray = toSorted
      def isAlreadySorted = as.toSeq == sortedArray.toSeq

      assert(isSorted(as, gt[Int]) == isAlreadySorted)
      assert(isSorted(sortedArray, gt[Int]) == true)
    }
  }

  behavior of "partial1"

  val plus = (_:Int) + (_:Int) //(x: Int, y: Int) => x + y
  val curriedPlus = plus.curried //(x: Int) => (y: Int) => x + y

  val append = (_:String) + (_:String)
  val curriedAppend = append.curried

  def asTuple[A,B] = (_:A, _:B)
  val curriedAsTuple = asTuple.curried

  it should "work" in {
    assert(partial1(1, plus)(3) == 4)
    assert(partial1("hello ", append)("world") == "hello world")
    assert(partial1(42, asTuple[Int,String])(" is the answer") == (42," is the answer"))
  }

  it should "work for random Ints" in {
    forAll("x", "y") { (x: Int, y: Int) =>
      assert(partial1(x, plus)(y) == plus(x,y))
    }
  }

  it should "work for random Strings" in {
    forAll("x", "y") { (x: String, y: String) =>
      assert(partial1(x, append)(y) == append(x,y))
    }
  }

  it should "work for random Ints and Strings" in {
    forAll("x", "y") { (x: Int, y: String) =>
      assertResult(asTuple(x,y))(partial1(x, asTuple[Int,String])(y))
    }
  }

  // curried to assist the compiler - yes, even Batman needs help sometimes!
  def checkForAll[A,B,C](f: (A,B) => C)(toTest: ((A,B) => C,A,B) => C)(implicit a: Arbitrary[A], b: Arbitrary[B]) = {
    forAll("x", "y") { (x: A, y: B) =>
      assertResult(f(x,y))(toTest(f,x,y))
    }
  }

  it should "work for random Strings and Ints" in {
    def toTest[A,B,C](f: (A,B) => C, x: A, y: B): C = partial1(x,f)(y)
    checkForAll(plus)(toTest)
    checkForAll(append)(toTest)
    checkForAll(asTuple[Int,String])(toTest)
  }

  behavior of "2.3 curry"

  it should "add 1 + 3 (1)" in {
    assertResult(4)(curry(plus)(1)(3))
  }

  it should "add two random numbers" in {
    forAll("x", "y") { (x: Int, y: Int) =>
      assertResult(x + y)(curry(plus)(x)(y))
      assertResult(x + y)(curry(plus)(y)(x))
    }
  }

  it should "work for random Strings and Ints" in {
    def toTest[A,B,C](f: (A,B) => C, x: A, y: B): C = curry(f)(x)(y)
    checkForAll(plus)(toTest)
    checkForAll(append)(toTest)
    checkForAll(asTuple[Int,String])(toTest)
  }

  behavior of "2.4 uncurry"

  it should "add 1 + 3 (2)" in {
    assertResult(4)(uncurry(curriedPlus)(1, 3))
  }

  it should "add two random numbers" in {
    forAll("x", "y") { (x: Int, y: Int) =>
      assertResult(x + y)(uncurry(curriedPlus)(x, y))
      assertResult(x + y)(uncurry(curriedPlus)(y, x))
    }
  }

  it should "work for random Strings and Ints" in {
    def toTest[A,B,C](f: (A,B) => C, x: A, y: B): C = uncurry(f.curried)(x,y)
    checkForAll(plus)(toTest)
    checkForAll(append)(toTest)
    checkForAll(asTuple[Int,String])(toTest)
  }

  behavior of "curry-uncurry"

  it should "add 1 + 3" in {
    assertResult(4)(curry(uncurry(curriedPlus))(1)(3))
    assertResult(4)(curry(uncurry(curriedPlus))(3)(1))
  }

  it should "always give the same result" in {
    forAll("x", "y") { (x: Int, y: Int) =>
      assertResult(x + y)(curry(uncurry(curriedPlus))(x)(y))
    }
  }

  it should "work for random Strings and Ints" in {
    def toTest[A,B,C](f: (A,B) => C, x: A, y: B): C = curry(uncurry(f.curried))(x)(y)
    checkForAll(plus)(toTest)
    checkForAll(append)(toTest)
    checkForAll(asTuple[Int,String])(toTest)
  }

  behavior of "uncurry-curry"

  it should "work" in {
    assertResult(4)(uncurry(curry(plus))(1, 3))
  }

  it should "work for random Strings and Ints" in {
    def toTest[A,B,C](f: (A,B) => C, x: A, y: B): C = uncurry(curry(f))(x,y)
    checkForAll(plus)(toTest)
    checkForAll(append)(toTest)
    checkForAll(asTuple[Int,String])(toTest)
  }

  behavior of "2.5 compose"

  def toString[T](t: T) = t.toString
  def neg[T](t: T)(implicit num: Numeric[T]) = num.negate(t)

  it should "work" in {
    assertResult("-42")(compose(toString[Int], neg[Int])(42))
    assertResult("-42.123")(compose(toString[Double], neg[Double])(42.123d))
  }

  it should "work for random Ints" in {
    forAll("x") { (x: Int) =>
      assertResult((-x).toString)(compose(toString[Int], neg[Int])(x))
    }
  }

  it should "work for random Doubles" in {
    forAll("x") { (x: Double) =>
      assertResult((-x).toString)(compose(toString[Double], neg[Double])(x))
    }
  }
}
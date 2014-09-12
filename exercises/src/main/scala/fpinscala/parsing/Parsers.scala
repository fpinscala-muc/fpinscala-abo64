package fpinscala.parsing

import scala.util.matching.Regex

import fpinscala.testing.Gen
import fpinscala.testing.Prop
import fpinscala.testing.Prop.forAll

trait Parsers[Parser[+_]] { self => // so inner classes may call methods of trait

  def run[A](p: Parser[A])(input: String): Either[ParseError,A] // 149, 163
//  def char(c: Char): Parser[Char] // 148, 149
//  def string(s: String): Parser[String] // 149
//  def orString(s1: String, s2: String): Parser[String] // 149

  def or[A](s1: Parser[A], s2: => Parser[A]): Parser[A] // 149, 156
  implicit def string(s: String): Parser[String] // 149
  implicit def operators[A](p: Parser[A]) = ParserOps[A](p) // 150
  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = // 150
    ParserOps(f(a))

  def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]] = // 150
    if (n <= 0) succeed(List())
    else map2(p, listOfN(n-1, p))(_ :: _)

  def many[A](p: Parser[A]): Parser[List[A]] = // 152, 155
    map2(p, many(p))(_ :: _) or succeed(List())

  def map[A,B](a: Parser[A])(f: A => B): Parser[B] = // 152
    flatMap(a)(f andThen succeed)

  def char(c: Char): Parser[Char] = // 153
    string(c.toString) map ((_: String).charAt(0))

  def succeed[A](a: A): Parser[A] //= // 153
//    string("") map ((_: String) => a)

  def slice[A](p: Parser[A]): Parser[String] // 154

  def many1[A](p: Parser[A]): Parser[List[A]] = // 154
    map2(p, many(p))(_ :: _)

  def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)] = // 154, 156, 157
    flatMap(p)(a => map(p2)(b => (a,b)))

  def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] = // 157
    flatMap(p)(a => map(p2)(b => f(a,b)))

  def flatMap[A,B](p: Parser[A])(f: A => Parser[B]): Parser[B] // 157

  implicit def regex(r: Regex): Parser[String] // 157

  case class ParserOps[A](p: Parser[A]) {
    def |[B>:A](p2: Parser[B]): Parser[B] = self.or(p,p2) // 150
    def or[B>:A](p2: => Parser[B]): Parser[B] = self.or(p,p2) // 150
    def map[A1 >: A, B](f: A1 => B): Parser[B] = self.map(p)(f) // 152
    def many[B >: A]: Parser[List[B]] = self.many(p) // 152
    def slice: Parser[String] = self.slice(p) // 154
    def **[B](p2: => Parser[B]): Parser[(A, B)] = self.product(p, p2) // 154
    def product[B](p2: => Parser[B]): Parser[(A, B)] = self.product(p, p2) // 154
    def flatMap[B](f: A => Parser[B]): Parser[B] = self.flatMap(p)(f) // 157
  }

  object Laws {
    def singleCharLaw(c: Char) = run(char(c))(c.toString) == Right(c) // 149
    def singleStringLaw(s: String) = run(string(s))(s) == Right(s) // 149

    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop = // 153
      forAll(in)(s => run(p1)(s) == run(p2)(s))
    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop = // 153
      equal(p, p.map((a:A) => a))(in)

    def succeedLaw[A](a: A)(s: String) = // 153
      run(succeed(a))(s) == Right(a)

    def numALaw[A](p: Parser[A], in: Gen[String]) = // 154
      equal(p.many.map((_: List[A]).size), slice(p.many).map((_: String).size))(in)

    def csListOfNLaw[A](p: Parser[A])(n: Gen[Int]): Prop = // 157
      forAll(n)(n => run(Exercises.csListOfN(p))(n + ("a" * n)).right.get.size == n)
  }

  object Facts {
    val numA: Parser[Int] = char('a').many.map((_: List[Char]).size) // 152
    val numA1 = char('a').many.slice.map((_: String).size) // 154

    val facts: Map[String,Boolean] = Map(
      """149: run(or(string("abra"),string("cadabra")))("abra") == Right("abra")""" ->
        (run(or(string("abra"), string("cadabra")))("abra") == Right("abra")), // 149
      """149: run(or(string("abra"), string("cadabra")))("cadabra") == Right("cadabra")""" ->
        (run(or(string("abra"), string("cadabra")))("cadabra") == Right("cadabra")),

      // seems to be a bug in the book: return type of listOfN is Parser[List[A]], not Parser[A]
      """150: run(listOfN(3, "ab" | "cad"))("ababcad") == Right(List("ab", "ab", "cad"))""" ->
        (run(listOfN(3, "ab" | "cad"))("ababcad") == Right(List("ab", "ab", "cad"))), // 150
      """150: run(listOfN(3, "ab" | "cad"))("cadabab") == Right(List("cad", "ab", "ab"))""" ->
        (run(listOfN(3, "ab" | "cad"))("cadabab") == Right(List("cad", "ab", "ab"))),
      """150: run(listOfN(3, "ab" | "cad"))("ababab") == Right(List("ab", "ab", "ab"))""" ->
        (run(listOfN(3, "ab" | "cad"))("ababab") == Right(List("ab", "ab", "ab"))),

      """154: run(numA)("aaa") == Right(3)""" ->
        (run(numA)("aaa") == Right(3)),
      """154: run(numA)("b") == Right(0)""" ->
        (run(numA)("b") == Right(0)),
      """154: run(numA1)("aaa") == Right(3)""" ->
        (run(numA1)("aaa") == Right(3)),
      """154: run(numA1)("b") == Right(0)""" ->
        (run(numA1)("b") == Right(0)),
      """154: run(slice((char('a') | char('b')).many))("aaba") == Right("aaba")""" ->
        (run(slice((char('a') | char('b')).many))("aaba") == Right("aaba")) // 154
    )
  }

  object Exercises {
    def map2ViaProduct[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] = // 154
//    product[A,B](p, p2) map ((ab:(A,B)) => {val (a,b) = ab; f(a,b)})
//    product[A,B](p, p2) map ((ab:(A,B)) => f.tupled(ab))
    product[A,B](p, p2) map f.tupled

    def csListOfN[A](p: Parser[A]): Parser[List[A]] = // 157
      regex("\\d+".r) flatMap (n => listOfN(n.toInt, p))
  }
}

// 161
case class Location(input: String, offset: Int = 0) {

  lazy val line = input.slice(0,offset+1).count(_ == '\n') + 1
  lazy val col = input.slice(0,offset+1).reverse.indexOf('\n')

  def toError(msg: String): ParseError =
    ParseError(List((this, msg)))

  def advanceBy(n: Int) = copy(offset = offset+n)

  /* Returns the line corresponding to this location */
  def currentLine: String = 
    if (input.length > 1) input.lines.drop(line-1).next
    else ""
}

// 163
case class ParseError(stack: List[(Location,String)] = List(),
                      otherFailures: List[ParseError] = List()) {
}
package fpinscala.datastructures

sealed trait Tree[+A]
case class Leaf[A](value: A) extends Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]


object Tree {

<<<<<<< HEAD
  def size(t: Tree[_]): Int = t match {
    case Leaf(_) => 1
    case Branch(l,r) => 1 + size(l) + size(r)
  }

//  def maximum(t: Tree[Int]): Int = t match {
//    case Leaf(n) => n
//    case Branch(l,r) => maximum(l) max maximum(r)
//  }

  def maximum[T](t: Tree[T])(implicit ev: Numeric[T]): T = t match {
    case Leaf(n) => n
    case Branch(l,r) => ev.max(maximum(l), maximum(r))
  }

  def depth(t: Tree[_]): Int =  t match {
    case Leaf(_) => 0
    case Branch(l,r) => (depth (l) max depth(r)) + 1
  }

  def map[A,B](t: Tree[A])(f: A => B): Tree[B] = t match {
    case Leaf(a) => Leaf(f(a))
    case Branch(l,r) => Branch(map(l)(f), map(r)(f))
  }

  def fold[A,B](t: Tree[A])(f: A => B)(g: (B,B) => B): B = t match {
    case Leaf(a) => f(a)
    case Branch(l,r) => g(fold(l)(f)(g), fold(r)(f)(g))
  }

  def sizeViaFold[A](t: Tree[A]): Int = 
    fold(t)(a => 1)(1 + _ + _)

  def maximumViaFold[T](t: Tree[T])(implicit ev: Numeric[T]): T = 
    fold(t)(a => a)(ev.max(_, _))

  def depthViaFold[A](t: Tree[A]): Int = 
    fold(t)(a => 0)((d1,d2) => 1 + (d1 max d2))
=======
  def size(t: Tree[_]): Int = sys.error("todo")

  def maximum(t: Tree[Int]): Int = sys.error("todo")

  def depth(t: Tree[_]): Int = sys.error("todo")
>>>>>>> 7755ca026a4f811472a9e7984c1acb9a3c17be35

  def map[A,B](t: Tree[A])(f: A => B): Tree[B] = sys.error("todo")

  def fold[A,B](t: Tree[A])(f: A => B)(g: (B,B) => B): B = sys.error("todo")

  def sizeViaFold[A](t: Tree[A]): Int = 
    fold(t)(a => 1)(1 + _ + _)

  def maximumViaFold[T](t: Tree[T])(implicit ev: Numeric[T]): T = sys.error("todo")

  def depthViaFold[A](t: Tree[A]): Int = sys.error("todo")
}

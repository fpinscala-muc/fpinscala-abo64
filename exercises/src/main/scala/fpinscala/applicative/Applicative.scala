package fpinscala
package applicative

import monads.Functor
import state._
import State._
import monoids._

trait Applicative[F[_]] extends Functor[F] { self =>

  implicit class ApplicativeOps[A](fa: F[A]) {
    def apply[B](fab: F[A => B]): F[B] = self.apply(fab)(fa)
    def map[B](f: A => B): F[B] = self.map(fa)(f)
  }

  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    // CAUTION: apply and map2 are defined mutually!
    // so one of the two must be overriden in an implementation!
    apply(map(fa)(f.curried))(fb)

  def apply[A, B](fab: F[A => B])(fa: F[A]): F[B] =
    //    map2(fab, fa)(_(_))
    map2(fab, fa)((f, x) => f(x))

  def unit[A](a: => A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B] =
    apply(unit(f))(fa)

  def sequence[A](fas: List[F[A]]): F[List[A]] =
    fas.foldRight(unit(List[A]()))((ma, mla) => map2(ma, mla)(_ :: _))

  def traverse[A, B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List[B]()))((a, mlb) => map2(f(a), mlb)(_ :: _))

  def replicateM[A](n: Int, fa: F[A]): F[List[A]] =
    //    if (n <= 0) unit(List[A]()) else map2(fa, replicateM(n - 1, fa))(_ :: _)
    sequence(List.fill(n)(fa))

  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    map2(fa, fb)((_, _))

  def map3[A, B, C, D](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => D): F[D] =
//    apply(apply(apply(unit(f.curried))(fa))(fb))(fc)
    fc apply (fb apply (fa apply unit(f.curried)))

  def map4[A, B, C, D, E](fa: F[A], fb: F[B], fc: F[C], fd: F[D])(f: (A, B, C, D) => E): F[E] =
    apply(apply(apply(apply(unit(f.curried))(fa))(fb))(fc))(fd)

  def factor[A, B](fa: F[A], fb: F[A]): F[(A, B)] = ???

  def product[G[_]](G: Applicative[G]): Applicative[({ type f[x] = (F[x], G[x]) })#f] ={
    val self = this
    new Applicative[({type f[x] = (F[x], G[x])})#f] {
      def unit[A](a: => A) = (self.unit(a), G.unit(a))
      override def apply[A,B](fs: (F[A => B], G[A => B]))(p: (F[A], G[A])) =
        (self.apply(fs._1)(p._1), G.apply(fs._2)(p._2))
    }
  }

  def compose[G[_]](G: Applicative[G]): Applicative[({ type f[x] = F[G[x]] })#f] = {
    val self = this
    new Applicative[({type f[x] = F[G[x]]})#f] {
      def unit[A](a: => A) = self.unit(G.unit(a))
      override def map2[A,B,C](fga: F[G[A]], fgb: F[G[B]])(f: (A,B) => C) =
        self.map2(fga, fgb)(G.map2(_,_)(f))
    }
  }

  def sequenceMap[K, V](ofa: Map[K, F[V]]): F[Map[K, V]] =
    ofa.foldLeft(unit(Map[K,V]())) {
      case (acc, (k, fv)) => apply(map(acc)(m =>
        (n: Map[K,V]) => m ++ n))(map(fv)((v: V) => Map(k -> v)))
    }
}

case class Tree[+A](head: A, tail: List[Tree[A]])

trait Monad[F[_]] extends Applicative[F] {
  def flatMap[A, B](ma: F[A])(f: A => F[B]): F[B] = join(map(ma)(f))

  def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)

  def compose[A, B, C](f: A => F[B], g: B => F[C]): A => F[C] =
    a => flatMap(f(a))(g)

  override def apply[A, B](mf: F[A => B])(ma: F[A]): F[B] =
    flatMap(mf)(f => map(ma)(a => f(a)))
}

object Monad {
  def eitherMonad[E]: Monad[({ type f[x] = Either[E, x] })#f] =
    new Monad[({ type f[x] = Either[E, x] })#f] {
      def unit[A](a: => A): Either[E, A] = Right(a)
      override def flatMap[A, B](e: Either[E, A])(f: A => Either[E, B]): Either[E, B] =
        e match {
          case Right(a) => f(a)
          case Left(l) => Left(l)
        }
      override def map[A,B](ea: Either[E,A])(f: A => B) = ea match {
        case Right(a) => Right(f(a))
        case Left(e) => Left(e)
      }
    }

  def stateMonad[S] = new Monad[({ type f[x] = State[S, x] })#f] {
    def unit[A](a: => A): State[S, A] = State(s => (a, s))
    override def flatMap[A, B](st: State[S, A])(f: A => State[S, B]): State[S, B] =
      st flatMap f
  }

  def composeM[F[_], N[_]](implicit F: Monad[F], N: Monad[N], T: Traverse[N]): Monad[({ type f[x] = F[N[x]] })#f] = ???
}

sealed trait Validation[+E, +A]

case class Failure[E](head: E, tail: Vector[E])
  extends Validation[E, Nothing]

case class Success[A](a: A) extends Validation[Nothing, A]

object Applicative {

  val listApplicative = new Applicative[List] {
    override def unit[A](a: => A): List[A] = List(a)
    override def map2[A, B, C](a: List[A], b: List[B])(f: (A, B) => C): List[C] =
      a zip b map f.tupled
  }

  val optionApplicative = new Applicative[Option] {
    override def unit[A](a: => A): Option[A] = Option(a)
    override def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = {
      def zip[A, B](a: Option[A], b: Option[B]): Option[(A, B)] = (a, b) match {
        case (Some(a), Some(b)) => Some((a, b))
        case _ => None
      }
      // product instead of zip would result in StackOverflow (circular definition)
      zip(a, b) map f.tupled
    }
  }

  val streamApplicative = new Applicative[Stream] {

    def unit[A](a: => A): Stream[A] =
      Stream.continually(a) // The infinite, constant stream

    override def map2[A, B, C](a: Stream[A], b: Stream[B])( // Combine elements pointwise
      f: (A, B) => C): Stream[C] =
      a zip b map f.tupled
  }

  def validationApplicative[E]: Applicative[({ type f[x] = Validation[E, x] })#f] =
    new Applicative[({ type f[x] = Validation[E, x] })#f] {
      def unit[A](a: => A) = Success(a)
      override def map2[A,B,C](fa: Validation[E,A], fb: Validation[E,B])(f: (A, B) => C) =
        (fa, fb) match {
          case (Success(a), Success(b)) => Success(f(a, b))
          case (Failure(h1,t1), Failure(h2,t2)) => Failure(h1, t1 ++ Vector(h2) ++ t2)
          case (e@Failure(_, _), _) => e
          case (_, e@Failure(_, _)) => e
        }
    }

  type Const[A, B] = A

  implicit def monoidApplicative[M](M: Monoid[M]) =
    new Applicative[({ type f[x] = Const[M, x] })#f] {
      def unit[A](a: => A): M = M.zero
      override def apply[A, B](m1: M)(m2: M): M = M.op(m1, m2)
    }
}

trait Traverse[F[_]] extends Functor[F] with Foldable[F] {
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] =
    sequence(map(fa)(f))
  def sequence[G[_]: Applicative, A](fma: F[G[A]]): G[F[A]] =
    traverse(fma)(ma => ma)

  type Id[A] = A
  val idMonad = new Monad[Id] {
    def unit[A](a: => A) = a
    override def flatMap[A, B](a: A)(f: A => B): B = f(a)
  }

  def map[A, B](fa: F[A])(f: A => B): F[B] =
    traverse[Id, A, B](fa)(f)(idMonad)

  import Applicative._

  override def foldMap[A, B](as: F[A])(f: A => B)(mb: Monoid[B]): B =
    traverse[({ type f[x] = Const[B, x] })#f, A, Nothing](
      as)(f)(monoidApplicative(mb))

  def traverseS[S, A, B](fa: F[A])(f: A => State[S, B]): State[S, F[B]] =
    traverse[({ type f[x] = State[S, x] })#f, A, B](fa)(f)(Monad.stateMonad)

  def mapAccum[S, A, B](fa: F[A], s: S)(f: (A, S) => (B, S)): (F[B], S) =
    traverseS(fa)((a: A) => (for {
      s1 <- get[S]
      (b, s2) = f(a, s)
      _ <- set(s2)
    } yield b)).run(s)

  override def toList[A](fa: F[A]): List[A] =
    mapAccum(fa, List[A]())((a, s) => ((), a :: s))._2.reverse

  def zipWithIndex[A](fa: F[A]): F[(A, Int)] =
    mapAccum(fa, 0)((a, s) => ((a, s), s + 1))._1

  def reverse[A](fa: F[A]): F[A] = ???

  override def foldLeft[A, B](fa: F[A])(z: B)(f: (B, A) => B): B = ???

  def fuse[G[_], H[_], A, B](fa: F[A])(f: A => G[B], g: A => H[B])(implicit G: Applicative[G], H: Applicative[H]): (G[F[B]], H[F[B]]) = ???

  def compose[G[_]](implicit G: Traverse[G]): Traverse[({ type f[x] = F[G[x]] })#f] = ???
}

object Traverse {
  val listTraverse = new Traverse[List] {
    override def traverse[M[_],A,B](as: List[A])(f: A => M[B])(implicit M: Applicative[M]): M[List[B]] =
      as.foldRight(M.unit(List[B]()))((a, fbs) => M.map2(f(a), fbs)(_ :: _))
  }

  val optionTraverse = new Traverse[Option] {
    override def traverse[M[_],A,B](oa: Option[A])(f: A => M[B])(implicit M: Applicative[M]): M[Option[B]] =
      oa match {
        case Some(a) => M.map(f(a))(Some(_))
        case None    => M.unit(None)
      }
  }

  val treeTraverse = new Traverse[Tree] {
    
  }
}

// The `get` and `set` functions on `State` are used above,
// but aren't in the `exercises` subproject, so we include
// them here
object StateUtil {

  def get[S]: State[S, S] =
    State(s => (s, s))

  def set[S](s: S): State[S, Unit] =
    State(_ => ((), s))
}

package cats

import cats.data.EitherK
import cats.tests.CatsSuite
import org.scalacheck._

class InjectTests extends CatsSuite {

  sealed trait Test1Algebra[A]

  case class Test1[A](value : Int, f: Int => A) extends Test1Algebra[A]

  object Test1Algebra {
    implicit def test1AlgebraAFunctor: Functor[Test1Algebra] =
      new Functor[Test1Algebra] {
        def map[A, B](a: Test1Algebra[A])(f: A => B): Test1Algebra[B] = a match {
          case Test1(k, h) => Test1(k, x => f(h(x)))
        }
      }

    implicit def test1AlgebraArbitrary[A](implicit seqArb: Arbitrary[Int], intAArb : Arbitrary[Int => A]): Arbitrary[Test1Algebra[A]] =
      Arbitrary(for {s <- seqArb.arbitrary; f <- intAArb.arbitrary} yield Test1(s, f))
  }

  sealed trait Test2Algebra[A]

  case class Test2[A](value : Int, f: Int => A) extends Test2Algebra[A]

  object Test2Algebra {
    implicit def test2AlgebraAFunctor: Functor[Test2Algebra] =
      new Functor[Test2Algebra] {
        def map[A, B](a: Test2Algebra[A])(f: A => B): Test2Algebra[B] = a match {
          case Test2(k, h) => Test2(k, x => f(h(x)))
        }
      }

    implicit def test2AlgebraArbitrary[A](implicit seqArb: Arbitrary[Int], intAArb : Arbitrary[Int => A]): Arbitrary[Test2Algebra[A]] =
      Arbitrary(for {s <- seqArb.arbitrary; f <- intAArb.arbitrary} yield Test2(s, f))
  }

  type T[A] = EitherK[Test1Algebra, Test2Algebra, A]

  test("inj & prj") {
    def distr[F[_], A](f1: F[A], f2: F[A])
                      (implicit
                       F: Functor[F],
                       I0: Test1Algebra :<: F,
                       I1: Test2Algebra :<: F): Option[Int] =
      for {
        Test1(x, _) <- I0.prj(f1)
        Test2(y, _) <- I1.prj(f2)
      } yield x + y

    forAll { (x: Int, y: Int) =>
      val expr1: T[Int] = Inject[Test1Algebra, T].inj(Test1(x, _ + 1))
      val expr2: T[Int] = Inject[Test2Algebra, T].inj(Test2(y, _ * 2))
      val res = distr[T, Int](expr1, expr2)
      res should ===(Some(x + y))
    }
  }

  test("apply & unapply") {
    def distr[F[_], A](f1: F[A], f2: F[A])
                      (implicit
                       F: Functor[F],
                       I0: Test1Algebra :<: F,
                       I1: Test2Algebra :<: F): Option[Int] =
      for {
        Test1(x, _) <- I0.unapply(f1)
        Test2(y, _) <- I1.unapply(f2)
      } yield x + y

    forAll { (x: Int, y: Int) =>
      val expr1: T[Int] = Inject[Test1Algebra, T].apply(Test1(x, _ + 1))
      val expr2: T[Int] = Inject[Test2Algebra, T].apply(Test2(y, _ * 2))
      val res = distr[T, Int](expr1, expr2)
      res should ===(Some(x + y))
    }
  }

  test("apply in left") {
    forAll { (y: Test1Algebra[Int]) =>
      Inject[Test1Algebra, T].inj(y) == EitherK(Left(y)) should ===(true)
    }
  }

  test("apply in right") {
    forAll { (y: Test2Algebra[Int]) =>
      Inject[Test2Algebra, T].inj(y) == EitherK(Right(y)) should ===(true)
    }
  }

  test("null identity") {
    val listIntNull = null.asInstanceOf[List[Int]]
    Inject.catsReflexiveInjectInstance[List].inj[Int](listIntNull) should ===(listIntNull)
    Inject.catsReflexiveInjectInstance[List].prj[Int](listIntNull) should ===(Some(listIntNull))
  }

}

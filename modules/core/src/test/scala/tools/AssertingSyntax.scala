package tools

import cats.Functor
import cats.syntax.functor._
import org.scalatest.{Assertion, Assertions, Succeeded}

trait AssertingSyntax { self: Assertions =>

  implicit class Asserting[F[_], A](private val self: F[A]) {

    /**
      * Asserts that the `F[A]` completes with an `A` and no exception is thrown.
      */
    def assertNoException(implicit F: Functor[F]): F[Assertion] =
      self.as(Succeeded)
  }

}

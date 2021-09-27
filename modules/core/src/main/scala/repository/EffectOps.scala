package repository

import cats.effect.{Async, ContextShift}

import scala.concurrent.Future

trait EffectOps {

  implicit class FutureToF[A](f: Future[A]) {

    def pureF[F[_] : Async : ContextShift]: F[A] = {
      Async.fromFuture(Async[F].delay {
        f
      })
    }
  }

}

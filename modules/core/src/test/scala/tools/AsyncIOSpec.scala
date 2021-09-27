package tools

import cats.effect.{ContextShift, Effect, IO, SyncIO, Timer}
import org.scalatest.{Assertion, AsyncTestSuite, Succeeded}

import scala.concurrent.{ExecutionContext, Future}

trait AsyncIOSpec extends AssertingSyntax { self: AsyncTestSuite =>
  /**
   * Helpers for testing IO async API
   * */
  override implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO]            = IO.contextShift(executionContext)
  implicit val ioTimer: Timer[IO]                          = IO.timer(executionContext)
  implicit val ioEffect: Effect[IO] = IO.ioEffect

  implicit def syncIoToFutureAssertion(io: SyncIO[Assertion]): Future[Assertion] = io.toIO.unsafeToFuture()
  implicit def ioToFutureAssertion(io: IO[Assertion]): Future[Assertion]         = io.unsafeToFuture()
  implicit def syncIoUnitToFutureAssertion(io: SyncIO[Unit]): Future[Assertion]  = io.toIO.as(Succeeded).unsafeToFuture()
  implicit def ioUnitToFutureAssertion(io: IO[Unit]): Future[Assertion]          = io.as(Succeeded).unsafeToFuture()
}

package scheduler

import cats.effect.{ Effect, Resource, Sync, Timer }
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

class StreamScheduler[F[_]: Effect] extends Scheduler[F] {

  def scheduleExecution(every: FiniteDuration)(func: => F[Unit])(implicit F: Sync[F],
                                                                 timer: Timer[F]): Resource[F, Unit] =
    Stream.eval(func).append(Stream.awakeEvery[F](every).evalMap(_ => func)).compile.resource.drain
}

object StreamScheduler {

  def apply[F[_]: Effect: Timer]: StreamScheduler[F] =
    new StreamScheduler
}

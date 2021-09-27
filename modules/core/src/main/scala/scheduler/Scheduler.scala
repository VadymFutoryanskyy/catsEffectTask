package scheduler

import cats.effect._

import scala.concurrent.duration._

trait Scheduler[F[_]] {

  def scheduleExecution(every: FiniteDuration)(func: => F[Unit])(implicit F: Sync[F],
                                                                 timer: Timer[F]): Resource[F, Unit]

}

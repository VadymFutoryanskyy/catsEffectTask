import Application.application
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ExitCode, URIO, ZIO, _}

object ZioMain extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO.runtime.flatMap { implicit r: Runtime[Clock with Blocking] =>
      application[Task].use(_ => Task.never).exitCode
    }

}

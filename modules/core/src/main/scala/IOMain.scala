import Application.application
import cats.effect._

object IOMain extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    application[IO].use(_ => IO.never).as(ExitCode.Success)
}

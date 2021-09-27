import cats.effect._
import cats.implicits._
import config.ServiceConf
import crawler.NyTimesCrawlerService
import graphql.{ GraphQL, QueryType, SangriaGraphQL }
import io.getquill.{ PostgresAsyncContext, SnakeCase }
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze._
import pureconfig._
import pureconfig.generic.auto._
import repository.HeadlineRepo
import routes.GraphQLRoutes
import scheduler.StreamScheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object Application {

  def graphQL[F[_]: Effect: ContextShift](
                                           repo: HeadlineRepo[F],
                                           blockingContext: ExecutionContext
                                         ): GraphQL[F] =
    SangriaGraphQL[F, HeadlineRepo[F]](
      QueryType.schema,
      repo.pure[F],
      blockingContext
    )

  def repository[F[_]: Effect: ContextShift](ctx: PostgresAsyncContext[SnakeCase.type]): HeadlineRepo[F] =
    HeadlineRepo.fromContext(ctx)

  def server[F[_]: ConcurrentEffect: ContextShift: Timer](conf: ServiceConf,
                                                          routes: HttpRoutes[F]): Resource[F, Server[F]] =
    BlazeServerBuilder[F](global)
      .bindHttp(conf.port, conf.host)
      .withHttpApp(routes.orNotFound)
      .resource

  def sqlContext[F[_]](blocker: Blocker)(implicit F: Sync[F],
                                         cs: ContextShift[F]): Resource[F, PostgresAsyncContext[SnakeCase.type]] =
    Resource.fromAutoCloseableBlocking(blocker)(F.delay {
      new PostgresAsyncContext(SnakeCase, "ctx")
    })

  def loadConfig[F[_]](implicit F: Sync[F]): Resource[F, ServiceConf] = {
    val config = ConfigSource.default.load[ServiceConf] match {
      case Left(e)      => F.raiseError(new Exception(e.toString()))
      case Right(value) => value.pure[F]
    }
    Resource.liftF(config)
  }

  def application[F[_]: ConcurrentEffect: ContextShift: Timer]: Resource[F, Server[F]] =
    for {
      config <- loadConfig[F]
      b <- Blocker[F]
      ctx <- sqlContext(b)
      repo    = repository(ctx)
      crawler = NyTimesCrawlerService(repo)
      gql     = graphQL[F](repo, b.blockingContext)
      rts     = GraphQLRoutes[F](gql)
      svr <- server[F](config, rts)
      _ <- StreamScheduler[F].scheduleExecution(config.scheduler)(crawler.crawl())
    } yield svr

}

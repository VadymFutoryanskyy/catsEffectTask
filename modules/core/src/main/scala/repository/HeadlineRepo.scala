package repository

import cats.effect.{Async, ContextShift}
import io.getquill.{PostgresAsyncContext, SnakeCase}
import logging.Logging
import model.Headline

import scala.concurrent.ExecutionContext.Implicits.global

trait HeadlineRepo[F[_]] {

  def fetchAll(): F[List[Headline]]

  def insertAll(headlines: List[Headline]): F[Unit]

  def removeAll(): F[Unit]
}

object HeadlineRepo extends EffectOps {

  def fromContext[F[_] : Async : ContextShift](ctx: PostgresAsyncContext[SnakeCase.type]): HeadlineRepo[F] = {
    import ctx._
    new HeadlineRepo[F] with Logging {
      override def fetchAll(): F[List[Headline]] = {
        log.info("Fetching all entities")
        ctx.run(query[Headline]).pureF
      }

      override def insertAll(headlines: List[Headline]): F[Unit] = {
        val insertQuery = quote {
          liftQuery(headlines).foreach(e => query[Headline].insert(e))
        }
        ctx.run(insertQuery).map(_ => log.info(s"Inserted ${headlines.size} items")).pureF
      }

      override def removeAll(): F[Unit] = {
        log.info("Removing all items")
        ctx.run(query[Headline].delete).map(_ => ()).pureF
      }
    }
  }
}

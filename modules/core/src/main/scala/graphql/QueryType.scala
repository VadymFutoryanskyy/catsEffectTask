package graphql

import cats.effect.Effect
import cats.effect.implicits._
import model.Headline
import repository.HeadlineRepo
import sangria.schema._
import sangria.macros.derive._

object QueryType {

  def apply[F[_]: Effect]: ObjectType[HeadlineRepo[F], Unit] = {

    val HeadLineType =
      deriveObjectType[Unit, Headline](ObjectTypeDescription("The headline"))

    ObjectType(
      "Query",
      fields[HeadlineRepo[F], Unit](
        Field(
          "news",
          ListType(HeadLineType),
          description = Some("Returns a list of all available headlines."),
          resolve = _.ctx.fetchAll().toIO.unsafeToFuture
        )
      )
    )
  }

  def schema[F[_]: Effect]: Schema[HeadlineRepo[F], Unit] =
    Schema(QueryType[F])

}

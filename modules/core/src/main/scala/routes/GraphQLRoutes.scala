package routes

import cats.effect._
import cats.implicits._
import graphql.GraphQL
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._

object GraphQLRoutes {

  def apply[F[_]: Sync: ContextShift](
      graphQL: GraphQL[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]; import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "graphql" ⇒
        req.as[Json].flatMap(graphQL.query).flatMap {
          case Right(json) => Ok(json)
          case Left(json) => BadRequest(json)
        }.handleErrorWith {
          e: Throwable => {
            val error = Json.obj(
              ("internal error", Json.fromString(e.toString)))
            BadRequest(error)
          }
        }
    }
  }
}

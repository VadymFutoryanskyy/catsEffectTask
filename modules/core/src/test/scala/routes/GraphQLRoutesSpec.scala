package routes

import cats.effect.{ContextShift, IO}
import graphql.GraphQL
import io.circe.Json
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class GraphQLRoutesSpec extends AnyFlatSpec with Matchers {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO]            = IO.contextShift(executionContext)

  it should "successfully get response" in {
    val expectedJson: Json = Json.obj(
      ("title", Json.fromString("link")))
    val successful = new GraphQL[IO] {
      override def query(request: Json): IO[Either[Json, Json]] = IO.pure(Right(expectedJson))
    }

    val response = GraphQLRoutes[IO](successful).orNotFound.run(
      Request(method = Method.POST, uri = uri"/graphql" ).withEntity(expectedJson)
    )

    check[Json](response, Status.Ok, Some(expectedJson))

  }

  it should "return bad response on Left result" in {
    val expectedJson: Json = Json.obj(
      ("error", Json.fromString("some error")))
    val failed = new GraphQL[IO] {
      override def query(request: Json): IO[Either[Json, Json]] = IO.pure(Left(expectedJson))
    }

    val response = GraphQLRoutes[IO](failed).orNotFound.run(
      Request(method = Method.POST, uri = uri"/graphql" ).withEntity(expectedJson)
    )

    check[Json](response, Status.BadRequest, Some(expectedJson))

  }

  it should "return bad response on failed IO" in {
    val expectedJson: Json = Json.obj(
      ("title", Json.fromString("link")))
    val successful = new GraphQL[IO] {
      override def query(request: Json): IO[Either[Json, Json]] = IO.raiseError(new RuntimeException("Boom"))
    }

    val response = GraphQLRoutes[IO](successful).orNotFound.run(
      Request(method = Method.POST, uri = uri"/graphql" ).withEntity(expectedJson)
    )

    check[Json](response, Status.BadRequest, None)

  }

  def check[A](actual:        IO[Response[IO]],
               expectedStatus: Status,
               expectedBody:   Option[A])(
                implicit ev: EntityDecoder[IO, A]
              ): Boolean =  {
    val actualResp         = actual.unsafeRunSync
    val statusCheck        = actualResp.status == expectedStatus
    val bodyCheck          = expectedBody.fold[Boolean](
      actualResp.body.compile.toVector.unsafeRunSync.isEmpty)( // Verify Response's body is empty.
      expected => actualResp.as[A].unsafeRunSync == expected
    )
    statusCheck && bodyCheck
  }


}

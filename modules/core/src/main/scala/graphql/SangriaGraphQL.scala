package graphql

import _root_.sangria.execution.{ExceptionHandler, Executor, HandledException, WithViolations}
import _root_.sangria.marshalling.circe._
import _root_.sangria.parser.{QueryParser, SyntaxError}
import cats.effect._
import cats.implicits._
import io.circe.Json
import io.circe.optics.JsonPath.root
import sangria.ast.Document
import sangria.schema.Schema
import sangria.validation.AstNodeViolation

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SangriaGraphQL {

  private val queryStringLens = root.query.string

  // Format a SyntaxError as a GraphQL `errors`
  private def formatSyntaxError(e: SyntaxError): Json =
    Json.obj(
      "errors" -> Json.arr(
        Json.obj(
          "message" -> Json.fromString(e.getMessage),
          "locations" -> Json.arr(
            Json.obj(
              "line" -> Json.fromInt(e.originalError.position.line),
              "column" -> Json.fromInt(e.originalError.position.column)
            )
          )
        )
      )
    )

  // Format a String as a GraphQL `errors`
  private def formatString(s: String): Json = Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(s))))

  // Format a Throwable as a GraphQL `errors`
  private def formatThrowable(e: Throwable): Json =
    Json.obj(
      "errors" -> Json
        .arr(Json.obj("class" -> Json.fromString(e.getClass.getName), "message" -> Json.fromString(e.getMessage)))
    )

  def apply[F[_], A](
                      schema: Schema[A, Unit],
                      userContext: F[A],
                      blockingExecutionContext: ExecutionContext
                    )(
                      implicit F: Async[F],
                    ): GraphQL[F] =
    new GraphQL[F] {

      def query(request: Json): F[Either[Json, Json]] = {
        val queryString = queryStringLens.getOption(request)
        queryString match {
          case Some(qs) => parseAndExecute(qs)
          case None => fail(formatString("No 'query' property was present in the request."))
        }
      }

      // Parse `query` and execute.
      private def parseAndExecute(query: String): F[Either[Json, Json]] =
        QueryParser.parse(query) match {
          case Success(ast) => exec(schema, userContext, ast)(blockingExecutionContext)
          case Failure(e: SyntaxError) => fail(formatSyntaxError(e))
          case Failure(e) => fail(formatThrowable(e))
        }

      // Lift a `Json` into the error side of our effect.
      private def fail(j: Json): F[Either[Json, Json]] =
        F.pure(j.asLeft)

      private def exec(
                        schema: Schema[A, Unit],
                        userContext: F[A],
                        query: Document
                      )(implicit ec: ExecutionContext): F[Either[Json, Json]] =
        userContext
          .flatMap { ctx =>
            F.async { (cb: Either[Throwable, Json] => Unit) =>
              Executor
                .execute(
                  schema = schema,
                  queryAst = query,
                  userContext = ctx,
                  exceptionHandler = ExceptionHandler {
                    case (_, e) ⇒ HandledException(e.getMessage)
                  }
                )
                .onComplete {
                  case Success(value) => cb(Right(value))
                  case Failure(error) => cb(Left(error))
                }
            }
          }
          .attempt
          .flatMap {
            case Right(json) => F.pure(json.asRight)
            case Left(err) => fail(formatThrowable(err))
          }
    }

}

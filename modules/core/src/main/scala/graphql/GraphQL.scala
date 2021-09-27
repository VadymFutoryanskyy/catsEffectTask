package graphql

import io.circe.Json

trait GraphQL[F[_]] {

  def query(request: Json): F[Either[Json, Json]]

}

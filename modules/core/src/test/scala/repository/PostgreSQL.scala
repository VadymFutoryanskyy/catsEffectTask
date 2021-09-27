package repository

import com.typesafe.config._
import org.testcontainers.containers.PostgreSQLContainer

class PostgreSQL(initScript: String) {

  private val container: PostgreSQLContainer[_] = {
    val psql: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:12-alpine").withInitScript(initScript)
    psql.start()
    psql
  }

  def stop() = {
    container.stop()
  }

  val config: Config = { //
    val components = List(
      container.getJdbcUrl,
      s"user=${container.getUsername}",
      s"password=${container.getPassword}"
    )
    ConfigFactory.empty().withValue(
      "url", ConfigValueFactory.fromAnyRef(components.mkString("&"))
    )
  }

}
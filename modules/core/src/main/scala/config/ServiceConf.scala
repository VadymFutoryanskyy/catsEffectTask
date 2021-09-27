package config

import scala.concurrent.duration.FiniteDuration

case class ServiceConf(
    host: String,
    port: Int,
    scheduler: FiniteDuration
)

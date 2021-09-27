package crawler

import cats.effect.Effect

trait CrawlerService[F[_]] {

  def crawl()(implicit E: Effect[F]): F[Unit]
}

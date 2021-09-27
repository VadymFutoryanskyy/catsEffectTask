package crawler

import cats.effect.{ Effect, Sync }
import cats.implicits._
import logging.Logging
import model.Headline
import net.ruippeixotog.scalascraper.browser.{ Browser, JsoupBrowser }
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import repository.HeadlineRepo

class NyTimesCrawlerService[F[_]](repo: HeadlineRepo[F], browser: Browser) extends CrawlerService[F] with Logging {
  // Could be moved to config, but this is NyTimes specific implementation so could live here
  val url              = "https://www.nytimes.com/"

  override def crawl()(implicit E: Effect[F]): F[Unit] = {
    log.info("Attempt to crawl ...")
    val crawlResult = for {
      doc <- E.delay(browser.get(url))
      headlines <- parseDocument(doc).pure[F]
      result <- persistHeadlines(headlines)
    } yield result
    crawlResult.handleError { e =>
      log.error(s"Error during crawling: $e")
    }
  }

  private def persistHeadlines(headlines: List[Headline])(implicit E: Sync[F]): F[Unit] =
    // Depending on business requirements we can update even if it's empty
    if (headlines.nonEmpty) {
      // In current implementation I just made it simple - if we have some items, then
      // first remove all old and insert new. Of course this logic could be
      // easily changed depending on requirements
      repo.removeAll().flatMap(_ => repo.insertAll(headlines))
    } else {
      E.unit
    }

  private def parseDocument(doc: browser.DocumentType): List[Headline] = {
    val headlineElements: List[Element] = doc >> elementList(".story-wrapper")
    headlineElements.flatMap { elem =>
      val maybeElement = elem >?> text("span")
      val mbHref       = elem >?> attr("href")("a")
      (maybeElement, mbHref) match {
        case (Some(headline), Some(href)) => List(Headline(headline, href))
        case _                            => List.empty
      }
    }
  }
}

object NyTimesCrawlerService {
  val browser: Browser = JsoupBrowser()
  def apply[F[_]](repo: HeadlineRepo[F]) = new NyTimesCrawlerService(repo, browser)
}

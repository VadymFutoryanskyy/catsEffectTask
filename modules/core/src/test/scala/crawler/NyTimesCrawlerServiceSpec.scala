package crawler

import cats.effect.IO
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupElement
import net.ruippeixotog.scalascraper.model.ElementQuery
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.flatspec.AsyncFlatSpec
import repository.HeadlineRepo
import tools.AsyncIOSpec

class NyTimesCrawlerServiceSpec extends AsyncFlatSpec with AsyncMockFactory with AsyncIOSpec {

  it should "not fail IO if exception was thrown" in {
    val browser = mock[JsoupBrowser]
    val repo = mock[HeadlineRepo[IO]]

    (repo.insertAll _).expects(*).never()
    (repo.removeAll _).expects().never()
    (browser.get _).expects(*).throws(new Exception("Boom!"))
    val underTest = new NyTimesCrawlerService(repo, browser)
    underTest.crawl().assertNoException
  }

  it should "not insert elements for empty list" in {
    val browser = mock[JsoupBrowser]
    val documentType = mock[browser.DocumentType]
    val element = mock[JsoupElement]

    val query = new ElementQuery[JsoupElement] {
      //Should not be called
      override def select(query: String): ElementQuery[JsoupElement] = ???

      override def iterator: Iterator[JsoupElement] = Iterator()
    }

    val repo = mock[HeadlineRepo[IO]]

    (repo.insertAll _).expects(*).never()
    (repo.removeAll _).expects().never()
    (browser.get _).expects(*).returning(documentType)
    (documentType.root _).expects().returning(element)
    (element.select _).expects(*).returning(query)
    val underTest = new NyTimesCrawlerService(repo, browser)
    underTest.crawl()
  }

  // Scraper library uses custom DSL and implicits which is quite hard to mock
  // so just leaving here partially completed test to show the way I would move to test it
  it should "successfully parse elements" in {
    val browser = mock[JsoupBrowser]
    val documentType = mock[browser.DocumentType]
    val element = mock[JsoupElement]
    val elementSecond = mock[JsoupElement]

    val query = new ElementQuery[JsoupElement] {
      //should not be called
      override def select(query: String): ElementQuery[JsoupElement] = ???

      override def iterator: Iterator[JsoupElement] = Iterator(elementSecond)
    }

    val repo = mock[HeadlineRepo[IO]]
    (repo.insertAll _).expects(*).never()
    (repo.removeAll _).expects().never()
    (browser.get _).expects(*).returning(documentType)
    (documentType.root _).expects().returning(element)
    (element.select _).expects(*).returning(query)
    val underTest = new NyTimesCrawlerService(repo, browser)
    underTest.crawl()
  }
}

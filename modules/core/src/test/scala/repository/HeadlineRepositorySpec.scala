package repository

import io.getquill.{PostgresAsyncContext, SnakeCase}
import model.Headline
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import tools.AsyncIOSpec

class HeadlineRepositorySpec extends AsyncFlatSpec with BeforeAndAfter with AsyncIOSpec with Matchers {

  val psqlServer = new PostgreSQL("sql/01.sql")

  it should "insert and fetch data correctly" in {
    val ctx = new PostgresAsyncContext(SnakeCase, psqlServer.config)

    val first = List(Headline("title1", "link1"))
    val second = List(Headline("title2", "link2"))

    val repo = HeadlineRepo.fromContext(ctx)
    for {
      zeroFetch <- repo.fetchAll()
      _ <- repo.insertAll(first)
      firstFetch <- repo.fetchAll()
      _ <- repo.removeAll()
      _ <- repo.insertAll(second)
      secondFetch <- repo.fetchAll()
      _ <- repo.removeAll()
      emptyFetch <- repo.fetchAll()
    } yield {
      zeroFetch.isEmpty shouldBe true
      firstFetch shouldBe first
      secondFetch shouldBe second
      emptyFetch.isEmpty shouldBe true
    }
  }

  after {
    psqlServer.stop()
  }

}



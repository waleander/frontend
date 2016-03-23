package feed

import com.gu.contentapi.client.model.ItemResponse
import common.{Edition, AkkaAgent}
import conf.LiveContentApi
import conf.LiveContentApi._
import controllers.Series
import model.{RelatedContent, Tag, RelatedContentItem}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MostPopularSeriesAgent {
  private val allSeries = Seq(
    "football/series/thefiver"
  )

  private val agent = AkkaAgent[Map[String, Map[String, Series]]](Map.empty)

  def get(edition: Edition, seriesId: String): Option[Series] = agent().get(seriesId).flatMap(_.get(edition.id))

  def refresh(): Unit = {
    for {
      seriesId <- allSeries
      edition <- Edition.all
    } yield {
      refresh(seriesId, edition)
    }
  }

  def refresh(seriesId: String, edition: Edition): Unit = {
    val popularFromCAPI: Future[Seq[ItemResponse]] = Future.sequence(popular(seriesId).map(url => getResponse(LiveContentApi.item(url, edition))))

    getResponse(LiveContentApi.item(seriesId, edition).showFields("all")).map { response =>
      response.tag.map { tag =>
        val popularFuture = popularFromCAPI.map(rs => rs.flatMap(_.content)).map(_.map(RelatedContentItem(_)))

        popularFuture onSuccess {
          case items: Seq[RelatedContentItem] => {
            agent.alter(current => current + (seriesId -> Map(edition.id -> Series(seriesId, Tag.make(tag), RelatedContent(items)))))
          }
        }
      }
    }
  }

  private def popular(seriesId: String): Seq[String] = {
    Map(
      "football/series/thefiver" -> Seq("football/2016/mar/09/the-fiver-zlatan-ibrahimovic-chelsea-psg-big-cup")
    ).get(seriesId).getOrElse(Nil)
  }
}

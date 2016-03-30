package model.commercial.hotels

import java.io.{ByteArrayInputStream, InputStream}
import java.lang.System._

import commercial.feeds.{FeedMetaData, MissingFeedException, ParsedFeed, SwitchOffException}
import common.{AkkaAgent, ExecutionContexts, Logging}
import org.apache.commons.io.input.BOMInputStream

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, _}
import scala.util.control.NonFatal
import scala.xml.XML

object HotelAgent extends ExecutionContexts with Logging {

  private lazy val hotelAgent = AkkaAgent[Seq[Hotel]](Nil)

  def availableHotels: Seq[Hotel] = hotelAgent.get

  def updateAvailableHotels(freshHotels: Seq[Hotel]): Future[Seq[Hotel]] = {
    hotelAgent.alter { oldHotels =>
      if (freshHotels.nonEmpty) {
        freshHotels
      } else {
        log.warn("Using old hotels data as there is no fresh hotels data")
        oldHotels
      }
    }
  }

  def hotelsByPlaceName(placeName: String): Seq[Hotel] = {
    availableHotels.filter(_.placeName == placeName).sortBy(_.overallRating)
  }

  def refresh(feedMetaData: FeedMetaData, feedContent: => Option[String]): Future[ParsedFeed[Hotel]] = {

    def ignoreByteOrderMarkAsStream(feed: String): InputStream = {
      new BOMInputStream(new ByteArrayInputStream(feed.getBytes()))
    }

    feedMetaData.switch.isGuaranteedSwitchedOn flatMap { switchedOn =>
      if (switchedOn) {
        val start = currentTimeMillis

        feedContent map { body =>
          val xml = XML.loadString(body.tail)
          val hotels =
            for {
              hotel <- xml \ "Hotel"
            } yield Hotel.fromXml(hotel)

          updateAvailableHotels(hotels)
          Future(ParsedFeed(hotels, Duration(currentTimeMillis - start, MILLISECONDS)))
        } getOrElse {
          Future.failed(MissingFeedException(feedMetaData.name))
        }
      } else {
        Future.failed(SwitchOffException(feedMetaData.switch.name))
      }
    } recoverWith {
      case NonFatal(e) => Future.failed(e)
    }
  }
}

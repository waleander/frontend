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
        freshHotels.sortWith(_.popularity < _.popularity)
      } else {
        log.warn("Using old hotels data as there is no fresh hotels data")
        oldHotels
      }
    }
  }

  def hotelsByCountry(countryName: String): Seq[Hotel] = availableHotels.filter(_.countryName == countryName)

  def hotelsByCountryDeduplicatedByPlace(countryName: String): Seq[Hotel] = {
    hotelsByCountry(countryName).foldLeft(Seq.empty[Hotel]){
      (result, current) =>
        if (result.forall(_.placeName != current.placeName))
          result :+ current
        else
          result
    }
  }

  def refresh(feedMetaData: FeedMetaData, feedContent: => Option[String]): Future[ParsedFeed[Hotel]] = {

    def ignoreByteOrderMarkAsStream(feed: String): InputStream = {
      new BOMInputStream(new ByteArrayInputStream(feed.getBytes()))
    }

    feedMetaData.parseSwitch.isGuaranteedSwitchedOn flatMap { switchedOn =>
      if (switchedOn) {
        val start = currentTimeMillis

        feedContent map { body =>
          val xml = XML.loadString(body.tail)
          val hotels =
            for {
              rawHotel <- xml \ "Hotel"
              hotel <- Hotel.fromXml(rawHotel)
            } yield hotel

          updateAvailableHotels(hotels)
          Future(ParsedFeed(hotels, Duration(currentTimeMillis - start, MILLISECONDS)))
        } getOrElse {
          Future.failed(MissingFeedException(feedMetaData.name))
        }
      } else {
        Future.failed(SwitchOffException(feedMetaData.parseSwitch.name))
      }
    } recoverWith {
      case NonFatal(e) => Future.failed(e)
    }
  }
}

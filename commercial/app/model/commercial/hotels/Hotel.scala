package model.commercial.hotels

import scala.language.implicitConversions
import scala.xml.NodeSeq

case class Hotel (
                   countryCode: String,
                   countryFileName: String,
                   countryName: String,
                   currencyCode: Option[String],
                   hotelFileName: String,
                   hotelID: String,
                   hotelName: String,
                   overallRating: Option[Double],
                   placeFileName: Option[String],
                   placeID: String,
                   placeName: String,
                   placeType: Option[String],
                   popularity: Int,
                   starRating: Option[Double],
                   stateName: Option[String],
                   statePlacefilename: Option[String],
                   statePlaceID: Option[String]
                 )

object Hotel {

  def fromXml(root: NodeSeq): Option[Hotel] = {

    implicit def popString[String](x: String) = x
    implicit def popBoolean[Boolean](x: String) = x.toBoolean
    def popDouble[Double](x: String) = x.toDouble
    def popInt[Int](x: String) = x.toInt

    def checkAndWrap[T](element: NodeSeq)(implicit op: String => T): Option[T] = {
      val elementValue = element.text
      if (elementValue.length == 0 || elementValue == "Null")
        None
      else {
        Some(op(elementValue))
      }
    }

    for {
      countryCode <- checkAndWrap[String](root \ "CountryCode")
      countryFileName <- checkAndWrap[String](root \ "CountryFileName")
      countryName <- checkAndWrap[String](root \ "CountryName")
      hotelFileName <- checkAndWrap[String](root \ "HotelFileName")
      hotelID <- checkAndWrap[String](root \ "HotelID")
      hotelName <- checkAndWrap[String](root \ "HotelName")
      placeID <- checkAndWrap[String](root \ "PlaceID")
      placeName <- checkAndWrap[String](root \ "PlaceName")
      popularity <- checkAndWrap[Int](root \ "Popularity")(popInt)
    } yield new Hotel(
        countryCode,
        countryFileName,
        countryName,
        currencyCode = checkAndWrap[String](root \ "CurrencyCode"),
        hotelFileName,
        hotelID,
        hotelName,
        overallRating = checkAndWrap[Double](root \ "OverallRating")(popDouble),
        placeFileName = checkAndWrap[String](root \ "PlaceFileName"),
        placeID,
        placeName,
        placeType = checkAndWrap[String](root \ "PlaceType"),
        popularity,
        starRating = checkAndWrap[Double](root \ "StarRating")(popDouble),
        stateName = checkAndWrap[String](root \ "StateName"),
        statePlacefilename = checkAndWrap[String](root \ "StatePlacefilename"),
        statePlaceID = checkAndWrap[String](root \ "StatePlaceID")
    )
  }
}

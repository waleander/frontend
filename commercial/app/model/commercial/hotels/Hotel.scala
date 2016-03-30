package model.commercial.hotels

import scala.language.implicitConversions
import scala.xml.NodeSeq

case class Hotel (
                   countryCode: Option[String],
                   countryFileName: Option[String],
                   countryName: Option[String],
                   currencyCode: Option[String],
                   hotelAddress: Option[String],
                   hotelFileName: Option[String],
                   hotelID: Option[String],
                   hotelName: Option[String],
                   hotelPostcode: Option[String],
                   latitude: Option[String],
                   longitude: Option[String],
                   minRate: Option[Double],
                   overallRating: Option[Double],
                   placeFileName: Option[String],
                   placeID: Option[String],
                   placeName: Option[String],
                   placeType: Option[String],
                   popularity: Option[Double],
                   propertyType: Option[String],
                   propertyTypeID: Option[String],
                   starRating: Option[Double],
                   stateName: Option[String],
                   statePlacefilename: Option[String],
                   statePlaceID: Option[String],
                   themes: Option[String],
                   trademarked: Option[Boolean]
                 )

object Hotel {

  def fromXml(root: NodeSeq): Hotel = {

    implicit def popString[String](x: String) = x
    implicit def popDouble[Double](x: String) = x.toDouble
    implicit def popBoolean[Boolean](x: String) = x.toBoolean

    def checkAndWrap[T](element: NodeSeq)(implicit op: String => T): Option[T] = {
      val elementValue = element.text
      if (elementValue.length == 0)
        None
      else {
        Some(op(elementValue))
      }
    }

    new Hotel(
      countryCode = checkAndWrap[String](root \ "countryCode"),
      countryFileName = checkAndWrap[String](root \ "countryFileName"),
      countryName = checkAndWrap[String](root \ "countryName"),
      currencyCode = checkAndWrap[String](root \ "currencyCode"),
      hotelAddress = checkAndWrap[String](root \ "hotelAddress"),
      hotelFileName = checkAndWrap[String](root \ "hotelFileName"),
      hotelID = checkAndWrap[String](root \ "hotelID"),
      hotelName = checkAndWrap[String](root \ "hotelName"),
      hotelPostcode = checkAndWrap[String](root \ "hotelPostcode"),
      latitude = checkAndWrap[String](root \ "latitude"),
      longitude = checkAndWrap[String](root \ "longitude"),
      minRate = checkAndWrap[Double](root \ "minRate"),
      overallRating = checkAndWrap[Double](root \ "overallRating"),
      placeFileName = checkAndWrap[String](root \ "placeFileName"),
      placeID = checkAndWrap[String](root \ "placeID"),
      placeName = checkAndWrap[String](root \ "placeName"),
      placeType = checkAndWrap[String](root \ "placeType"),
      popularity = checkAndWrap[Double](root \ "popularity"),
      propertyType = checkAndWrap[String](root \ "propertyType"),
      propertyTypeID = checkAndWrap[String](root \ "propertyTypeID"),
      starRating = checkAndWrap[Double](root \ "starRating"),
      stateName = checkAndWrap[String](root \ "stateName"),
      statePlacefilename = checkAndWrap[String](root \ "statePlacefilename"),
      statePlaceID = checkAndWrap[String](root \ "statePlaceID"),
      themes = checkAndWrap[String](root \ "themes"),
      trademarked = checkAndWrap[Boolean](root \ "trademarked")
    )
  }
}

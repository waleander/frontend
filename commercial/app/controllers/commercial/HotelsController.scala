package controllers.commercial

import common.ExecutionContexts
import model.NoCache
import model.commercial.hotels.HotelAgent
import play.api.mvc._

import scala.concurrent.Future

object HotelsController
  extends Controller
    with ExecutionContexts
    with implicits.Requests {

  def renderHotel = Action.async { implicit request =>
      specificId map { hotelId =>
        Future {
          val clickMacro = request.getParameter("clickMacro")
          val omnitureId = request.getParameter("omnitureId")
          val selectedHotels = HotelAgent.hotelsByCountryDeduplicatedByPlace(hotelId)

          selectedHotels match {
            case Nil => NoCache(jsonFormat.nilResult)
            case hotels =>
              jsonFormat.result(views.html.hotels.hotel(hotels.take(4)))
          }
        }
      } getOrElse {
        Future.successful((NoCache(jsonFormat.nilResult)))
      }
  }
}

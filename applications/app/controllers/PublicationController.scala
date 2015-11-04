package controllers

import com.gu.contentapi.client.model.{Content => ApiContent}
import common._
import conf.LiveContentApi.getResponse
import conf._
import conf.switches.Switches
import implicits.Dates
import model.{Content, Section}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.{Action, Controller, RequestHeader, Result}
import play.twirl.api.Html
import services.IndexPage
import views.support.RenderOtherStatus

import scala.concurrent.Future

object PublicationController extends Controller with ExecutionContexts with RendersItemResponse with Dates with Logging {
  private val dateFormatUTC = DateTimeFormat.forPattern("yyyy/MMM/dd").withZone(DateTimeZone.UTC)

  def render(publication: String, path: String) = Action.async { implicit request =>
    renderItem(publication, path)
  }

  def renderDate(publication: String, path: String, year: String, month: String, day: String) = Action.async { implicit request =>
    renderItem(publication, path, requestedDate(s"$year/$month/$day").withTimeAtStartOfDay())
  }

  def renderItem(publication: String, path: String, date: DateTime)(implicit request: RequestHeader): Future[Result] = {
    lookup(publication, path, date) map {
      case Left(model) => renderPublicationPages(model)
      case Right(other) => RenderOtherStatus(other)
    }
  }

  private def requestedDate(dateString: String) = {
    dateFormatUTC
      .parseDateTime(dateString)
      .withTimeAtStartOfDay()
      .toDateTime
  }

  private def lookup(publication: String,
                     path: String,
                     date: DateTime = requestedDate(DateTime.now.toString).withTimeAtStartOfDay())
                    (implicit request: RequestHeader) = {

    getResponse(LiveContentApi.item(publication + "/" + path, Edition(request))
        .contentSet("print-sent")
        .fromDate(date)
        .toDate(date)
        .useDate("newspaper-edition")
    ).map { response =>
      val model = response.section.map { section =>
        IndexPage(Section(section), response.results.map(Content(_)))
      }
      ModelOrResult(model, response)
    }.recover {convertApiExceptions}
  }

  private def renderPublicationPages(model: IndexPage)(implicit request: RequestHeader) = {
    val htmlResponse: (() => Html) = () => views.html.index(model)
    val jsonResponse = () => views.html.fragments.indexBody(model)
    renderFormat(htmlResponse, jsonResponse, model.page, Switches.all)
  }

}

package controllers

import play.api.mvc._


class DocumentaryController extends Controller {

  def renderDoc() = Action { request => Ok(views.html.documentaryPage("hello Sir!")(request)) }

}

object DocumentaryController extends DocumentaryController

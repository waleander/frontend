package controllers.admin

import common.ExecutionContexts
import play.api.mvc._
import services.AbTestApi
import model.NoCache

object AbTestApiController extends Controller with ExecutionContexts {
  def abTest(id: Option[String]) = AuthActions.AuthActionTest.async { request =>
    AbTestApi.abTest(id) map (body => NoCache(Ok(body) as "application/json"))
  }
}

package services

import com.gu.abacus.models.{Mvt, Platform}
import common.ExecutionContexts
import conf.Configuration._
import play.api.libs.ws.WS
import play.api.Play.current

import scala.concurrent.Future

object AbacusApi extends ExecutionContexts {
  def testsFor(platform: Platform): Future[Option[Seq[Mvt]]] = {
    WS.url(abacus.host)
      .withQueryString("platform" -> platform.toString)
      .get map(_.json.validate[Seq[Mvt]].asOpt)
  }
}

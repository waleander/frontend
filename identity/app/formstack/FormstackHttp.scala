package formstack

import com.google.inject.Singleton
import common.ExecutionContexts
import play.api.Play.current
import play.api.libs.ws.{WSResponse, WS}

import scala.concurrent.Future

@Singleton
class WsFormstackHttp extends ExecutionContexts {
  def get(url: String, parameters: Seq[(String, String)] = Nil): Future[WSResponse] = {
    WS.url(url)
      .withRequestTimeout(2000)
      .withQueryString(parameters:_*)
      .get()}
}

package formstack

import com.google.inject.Singleton
import common.ExecutionContexts
import play.api.Play.current
import play.api.libs.ws.WS

import scala.concurrent.Future

case class FormstackHttpResponse(body: String, status: Int, statusText: String)

@Singleton
class WsFormstackHttp extends ExecutionContexts {
  def GET(url: String, parameters: Seq[(String, String)] = Nil): Future[FormstackHttpResponse] = {
    WS.url(url)
      .withRequestTimeout(2000)
      .withQueryString(parameters:_*)
      .get()
      .map(response => FormstackHttpResponse(response.body, response.status, response.statusText))
  }
}

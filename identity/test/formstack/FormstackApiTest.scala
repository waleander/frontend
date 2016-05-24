package formstack

import org.scalatest.{EitherValues, ShouldMatchers, FunSuite, path}
import org.scalatest.mock.MockitoSugar
import idapiclient.IdDispatchAsyncHttpClient
import org.mockito.Mockito._
import org.mockito.Matchers.any
import client.Parameters
import client.connection.HttpResponse
import play.api.libs.ws.{WSRequest, WSClient, WSResponse}
import scala.concurrent.Future
import common.ExecutionContexts


class FormstackApiTest extends path.FreeSpec with ShouldMatchers with MockitoSugar with ExecutionContexts {
  val httpClient = mock[WSClient]
  val httpRequest = mock[WSRequest]

  when(httpRequest.withRequestTimeout(any[Int]())).thenReturn(httpRequest)
  when(httpRequest.withQueryString(any[(String, String)]())).thenReturn(httpRequest)
  when(httpClient.url(any[String]())).thenReturn(httpRequest)

  val formstackApi = new FormstackApi(httpClient)

  val validBody = """{"id":"123456","name":"Test","viewkey":"abcdef","views":"1000","created":"2013-12-13 07:46:08","deleted":"0","submissions":"100","submissions_unread":"0","submissions_today":0,"last_submission_id":"123456","last_submission_time":"2013-12-13 09:27:48","url":"https:\/\/www.formstack.com\/forms\/?123456-abcdef","data_url":"","summary_url":"","rss_url":"","encrypted":false,"thumbnail_url":"https:\/\/formstack.com\/test\/","submit_button_title":"Test","inactive":false,"timezone":"Europe\/London","permissions":"full","folder":"Test folder","javascript":"<script type=\"text\/javascript\" src=\"https:\/\/www.formstack.com\/forms\/js.php?123456-abcdef-v1\"><\/script><noscript><a href=\"https:\/\/www.formstack.com\/forms\/?123456-abcdef\" title=\"Online Form\">Online Form - Test<\/a><\/noscript>","html":"<html><\/html>","fields":[]}"""
  val inactiveBody = """{"id":"123456","name":"Test","viewkey":"abcdef","views":"1000","created":"2013-12-13 07:46:08","deleted":"0","submissions":"100","submissions_unread":"0","submissions_today":0,"last_submission_id":"123456","last_submission_time":"2013-12-13 09:27:48","url":"https:\/\/www.formstack.com\/forms\/?123456-abcdef","data_url":"","summary_url":"","rss_url":"","encrypted":false,"thumbnail_url":"https:\/\/formstack.com\/test\/","submit_button_title":"Test","inactive":true,"timezone":"Europe\/London","permissions":"full","folder":"Test folder","javascript":"<script type=\"text\/javascript\" src=\"https:\/\/www.formstack.com\/forms\/js.php?123456-abcdef-v1\"><\/script><noscript><a href=\"https:\/\/www.formstack.com\/forms\/?123456-abcdef\" title=\"Online Form\">Online Form - Test<\/a><\/noscript>","html":"<html><\/html>","fields":[]}"""

  "the checkForm method" - {
    "returns the validated form if the API says it is ok" in {
      val formstackForm = FormstackForm("123456", "abcdef", None)
      val response = mock[WSResponse]
      when(response.body).thenReturn(validBody)
      when(response.status).thenReturn(200)
      when(response.statusText).thenReturn("ok")
      when(httpRequest.get()).thenReturn(Future.successful(response))

      formstackApi.checkForm(formstackForm).map {
        case Left(errors) => fail(s"expected Right, got errors, $errors")
        case Right(f) => f should equal(formstackForm)
      }
    }

    "returns an error if the form is not active" in {
      val formstackForm = FormstackForm("123456", "abcdef", None)
      val response = mock[WSResponse]
      when(response.body).thenReturn(inactiveBody)
      when(response.status).thenReturn(200)
      when(response.statusText).thenReturn("ok")
      when(httpRequest.get()).thenReturn(Future.successful(response))

      formstackApi.checkForm(formstackForm).map {
        case Right(f) => fail(s"expected Left, got Right")
        case Left(errors) => errors.exists(_.description.contains("inactive: false")) should equal(true)
      }
    }

    "returns an error if the API responds poorly" in {
      val formstackForm = FormstackForm("123456", "abcdef", None)
      val response = mock[WSResponse]
      when(response.body).thenReturn("")
      when(response.status).thenReturn(405)
      when(response.statusText).thenReturn("Method not allowed")
      when(httpRequest.get()).thenReturn(Future.successful(response))

      formstackApi.checkForm(formstackForm).map {
        case Right(f) => fail(s"expected Left, got Right")
        case Left(errors) => errors.size should be > 0
      }
    }
  }
}

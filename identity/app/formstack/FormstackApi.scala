package formstack

import client.parser.JodaJsonSerializer
import client.{Error, Response}
import com.google.inject.{Inject, Singleton}
import com.gu.identity.model.LiftJsonConfig
import common.ExecutionContexts
import conf.Configuration
import net.liftweb.json._
import play.api.libs.ws.{WSRequest, WSClient}
import utils.SafeLogging
import scala.concurrent.Future

@Singleton
class FormstackApi @Inject()(httpClient: WSClient) extends ExecutionContexts with SafeLogging {

  implicit val formats = LiftJsonConfig.formats + new JodaJsonSerializer

  private def formstackGet(url: String, oauthToken: String): WSRequest =
    httpClient
      .url(url)
      .withRequestTimeout(2000)
      .withQueryString("oauth_token" -> oauthToken)

  def formstackUrl(formId: String) = {
    val formstackUrl = Configuration.formstack.url
    s"$formstackUrl/form/$formId.json"
  }

  def checkForm(formstackForm: FormstackForm): Future[Response[FormstackForm]] = {
    formstackGet(formstackUrl(formstackForm.formId), Configuration.formstack.oAuthToken).get.map { response =>
        response.status match {
          case 200 =>
            logger.trace("Formstack API returned 200 for reference lookup")
            val json: JValue = parse(response.body)
            (for {
              formId <- (json \ "id").extractOpt[String]
              inactive <- (json \ "inactive").extractOpt[Boolean]
            } yield {
              if (formstackForm.formId == formId && !inactive) {
                logger.trace(s"Formstack reference $formId was good")
                Right(formstackForm)
              } else {
                logger.warn(s"Form, '$formId' is valid but not enabled (request formId vs response formId: ${formstackForm.formId} - $formId, inactive: $inactive)")
                Left(List(Error("Invalid form", "This is not a valid form", 404)))
              }
            }).getOrElse {
              logger.warn(s"200 received from Formstack for '${formstackForm.formId}', but response was invalid $response.body")
              Left(List(Error("Invalid Formstack API response", "")))
            }
          case 405 =>
            logger.warn("405 returned while checking formstack reference")
            Left(List(Error("Invalid form reference", "Invalid form reference", 405)))
          case 404 =>
            logger.warn(s"Attempted to load bad formstack reference (404) $response.body")
            Left(List(Error("Form not found", "Form not found", 404)))
          case _ =>
            logger.warn(s"Unexpected error getting info for formstack reference. Status code ${response.status}, body $response.body")
            Left(List(Error("Form error", "Unexpected error retrieving form info", response.status)))
        }
      }
    }
}

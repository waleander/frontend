package services

import conf.Configuration.abTestApi
import scala.concurrent.Future
import common.{BadConfigurationException, ExecutionContexts, Logging}
import play.api.libs.json._
import play.api.libs.ws.WS
import implicits.Strings

object AbTestDefinition {
  implicit val jsonReads = Json.reads[AbTestDefinition]
  implicit val jsonWrites = Json.writes[AbTestDefinition]
}

case class AbTestDefinition(id: String, urlEncodedJs: String)

object AbTestApi extends ExecutionContexts with Logging with implicits.WSRequests with Strings {

  import play.api.Play.current

  private def getLocalTest(id: String): Future[JsValue] = {
    val viewabilityJs = """define(function () {
                        |
                        |    return function () {
                        |        this.id = 'Viewability';
                        |        this.start = '2015-06-15';
                        |        this.expiry = '2015-08-01';
                        |        this.author = 'Steve Vadocz';
                        |        this.description = 'Viewability - Includes whole viewability package: ads lazy loading, sticky header, sticky MPU, spacefinder 2.0, dynamic ads, ad next to comments';
                        |        this.audience = 0.1;
                        |        this.audienceOffset = 0.5;
                        |        this.successMeasure = '';
                        |        this.audienceCriteria = 'Audience from all editions';
                        |        this.dataLinkNames = '';
                        |        this.idealOutcome = 'Increased user engagement and commercial viewability';
                        |
                        |        this.canRun = function () {
                        |            return true;
                        |        };
                        |
                        |        this.variants = [
                        |            {
                        |                id: 'control',
                        |                test: function () {}
                        |            },
                        |            {
                        |                id: 'variant',
                        |                test: function () {}
                        |            }
                        |        ];
                        |    };
                        |
                        |});
                        |""".stripMargin
    val testId = "viewability"
    val jsValue = Json.toJson(AbTestDefinition(testId, viewabilityJs))

    Future(jsValue)

  }

  private def getRemoteTest(id: String) = {
    val maybeJson = for {
      host <- abTestApi.abTestApiHost
      key <- abTestApi.abTestApiKey
    } yield {
      val url = if (id.nonEmpty) {
        s"$host/ab/$id?key=$key"
      } else {
        s"$host/all?key=$key"
      }
      log.info(s"Making request to AB Test API: $url")
      WS.url(url).withRequestTimeout(2000).getOKResponse().map(_.json)
    }

    maybeJson.getOrElse {
      Future.failed(new BadConfigurationException("AB Test API host or key not configured"))
    }
  }

  def abTest(id: String) = {
    abTestApi.abTestApiHost match {
      case Some(hostName) => getRemoteTest(id)
      case _ => getLocalTest(id)
    }
  }
}

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

case class AbTestDefinition(id: String, js: String)

object AbTestApi extends ExecutionContexts with Logging with implicits.WSRequests with Strings {

  import play.api.Play.current

  private val viewability = AbTestDefinition(
    "Viewability",
    """function Viewability() {
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
      |}""".stripMargin )

  private val pinterest = AbTestDefinition(
    "Pintrest",
    """function Pintrest() {
      |        this.id = 'Pintrest';
      |        this.start = '2015-07-01';
      |        this.expiry = '2015-07-16';
      |        this.author = 'Stephan Fowler';
      |        this.description = 'Page-level Pintrest buttons on content pages';
      |        this.audience = 0.1;
      |        this.audienceOffset = 0.9;
      |        this.successMeasure = 'Pintrest shares per visit';
      |        this.audienceCriteria = '';
      |        this.dataLinkNames = '';
      |        this.idealOutcome = 'More Pintrest shares per visit, in turn leading to more Pintrest referrals.';
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
      |    }""".stripMargin )

  private val tests: List[AbTestDefinition] = List(viewability, pinterest).sortWith(_.id < _.id)

  private def getLocalTest(id: String): Future[JsValue] = {
    val jsValue = id.nonEmpty match {
      case true => Json.toJson(tests.filter(_.id.toLowerCase == id.toLowerCase).map(test => AbTestDefinition(test.id, test.js)))
      case _ => Json.toJson(tests.map(test => AbTestDefinition(test.id, test.js)))
    }
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

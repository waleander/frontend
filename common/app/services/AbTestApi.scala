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

  private def getLocalTest(id: Option[String]): Future[JsValue] = {
    val headlinesJs =
      """|define([
        |    'common/utils/_',
        |    'bonzo',
        |    'fastdom',
        |    'qwery'
        |], function (
        |    _,
        |    bonzo,
        |    fastdom,
        |    qwery
        |) {
        |    /**
        |     * DO NOT MOVE THIS INSIDE THE `tests` FOLDER
        |     * ------------------------------------------
        |     * Rich has written some funky thing that parses the JavaScript in that folder in order to provide an endpoint with
        |     * test IDs. As we're programmatically generating test IDs here, the parser gets confused, and then provides a test
        |     * without an ID, which breaks the data team's loader. As this is a temporary thing, let's just not send it to the
        |     * data team at all to begin with.
        |     */
        |    return function (n) {
        |        this.id = 'Headline' + n;
        |        this.start = '2015-04-9';
        |        this.expiry = '2015-07-17';
        |        this.author = 'Robert Berry';
        |        this.description = 'A/B test for headline number ' + n;
        |        this.audience = 0.01;
        |        this.audienceOffset = 0.75 + 0.01 * n;
        |        this.successMeasure = 'Greater page views per visit';
        |        this.audienceCriteria = '1% of our audience, only on fronts';
        |        this.dataLinkNames = '';
        |        this.idealOutcome = '';
        |
        |        this.canRun = function () {
        |            return true;
        |        };
        |
        |        function setHeadlineVariant(i) {
        |            _.forEach(qwery('.js-a-b-headline-' + n), function (el) {
        |                var $el = bonzo(el),
        |                    headlineEls = qwery('.js-headline-text', el),
        |                    variantHeadline = JSON.parse($el.attr('data-headline-variants'))[i];
        |
        |                fastdom.write(function () {
        |                    _.forEach(headlineEls, function (headlineEl) {
        |                        bonzo(headlineEl).html(variantHeadline);
        |                    });
        |                });
        |            });
        |        }
        |
        |        this.variants = [
        |            {
        |                id: 'a',
        |                test: function () {}
        |            },
        |            {
        |                id: 'b',
        |                test: function () {
        |                    setHeadlineVariant(0);
        |                }
        |            }
        |        ];
        |    };
        |});""".stripMargin
    val testId = "headlines"
    val jsValue = Json.toJson(AbTestDefinition(testId, headlinesJs.urlEncoded))

    Future(jsValue)

  }

  private def getRemoteTest(id: Option[String]) = {
    val maybeJson = for {
      host <- abTestApi.abTestApiHost
      key <- abTestApi.abTestApiKey
    } yield {
      val url = id match {
        case Some(testId) => s"$host/ab/$testId?key=$key"
        case _ => s"$host/all?key=$key"
      }
      log.info(s"Making request to AB Test API: $url")
      WS.url(url).withRequestTimeout(2000).getOKResponse().map(_.json)
    }

    maybeJson.getOrElse {
      Future.failed(new BadConfigurationException("AB Test API host or key not configured"))
    }
  }

  def abTest(id: Option[String]) = {
    val jsTests = abTestApi.abTestApiHost match {
      case Some(hostName) => getRemoteTest(id)
      case _ => getLocalTest(id)
    }
    jsTests.map(_.as[Seq[AbTestDefinition]])
  } 

}

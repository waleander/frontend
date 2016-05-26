package pagepresser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Play.current
import play.api.libs.ws.WS

import scala.collection.JavaConversions._
import scala.concurrent.Future

object NextGenInteractiveHtmlCleaner extends HtmlCleaner with implicits.WSRequests {

  override def canClean(document: Document): Boolean = {
    document.getElementById("interactive-content") != null &&
      document.getElementsByAttributeValue("rel","canonical").attr("href").toLowerCase.contains("/ng-interactive/")
  }

  override def clean(document: Document) = {
    rewriteTemplate(document)
  }

  private def rewriteTemplate(document: Document) = {

    val previewUrl = document.getElementsByAttributeValue("rel","canonical")
      .attr("href")
      .replaceAll("http(s)?://www.theguardian.com","")
      .toLowerCase

    val ngTemplate = loadNextGenPreview(previewUrl)

    ngTemplate.map{ tpl =>
      println(s"### ${this.getClass.getCanonicalName} RE-WRITING...")

      val srcDoc = document.clone()
      val templateDoc = Jsoup.parse(tpl.body)

      document.head().replaceWith(templateDoc.head())
      document.body().replaceWith(templateDoc.body())
      document.title(srcDoc.title())
      moveMeta(srcDoc, document)
//    moveHeadElements(srcDoc, "link", "rel", "canonical", document)
//    moveHeadElements(srcDoc, "link", "rel", "shorturl", document)
//    moveHeadElements(srcDoc, "link", "rel", "publisher", document)
//    moveHeadElements(srcDoc, "link", "rel", "stylesheet", document)

      // global replace all urls with https:
      val securedDoc = Jsoup.parse(securedSource(document.html()))

      document.head().replaceWith(securedDoc.head())
      document.body().replaceWith(securedDoc.body())

      addJqueryScript(document)
//    addRequireJsScript(document)
      universalClean(document)
      removeByTagName(document, "noscript")
      removeInsecureScripts(document)

      println(s"### ${this.getClass.getCanonicalName} RE-WRITTEN!")
      document
    }
  }

  private def securedSource(src: String): String = {
    src.replaceAllLiterally(""""//""", """"https://""").replaceAllLiterally("'//", "'https://").replaceAllLiterally("http://", "https://")
  }

  private def getChildElementsOf(document: Document, tag: String, attributeKey: String, attributeVal: String) = {
    document.getElementsByTag(tag).filter { el =>
      el.hasAttr(attributeKey) && el.attr(attributeKey) == attributeVal
    }.flatMap(_.children())
  }

  private def moveMeta(fromDoc: Document,
                       toDoc: Document): Document = {
    toDoc.getElementsByTag("meta").foreach(_.remove)
    fromDoc.getElementsByTag("meta").foreach { el =>
      val newMeta = toDoc.head().prependElement("meta")
      for (attr <- el.attributes()) {
        newMeta.attr(attr.getKey, securedSource(attr.getValue))
      }
    }
    toDoc
  }

  private def moveHeadElements(fromDoc: Document,
                               tag: String,
                               //attributes: Seq[(String, String)],
                               attributeKey: String,
                               attributeVal: String,
                               toDoc: Document): Document = {

    //toDoc.getElementsByTag(tag).filter(el => el.hasAttr(attributeKey) && el.attr(attributeKey) == attributeVal).foreach(_.remove)

    fromDoc.getElementsByAttributeValue(attributeKey, attributeVal).foreach{ el =>
      val newEl = toDoc.head().prependElement(tag)
      for (attr <- el.attributes()) {
        newEl.attr(attr.getKey, securedSource(attr.getValue))
      }
    }
    toDoc
  }

  private def loadNextGenPreview(urlIn: String) = {
    val previewUrl = "http://interactive.guim.co.uk/preview/#http://interactive.guim.co.uk/next-gen" + urlIn + "/boot.js"
    val wsRequest = WS.url(previewUrl)

    log.info(s"Calling ${wsRequest.uri}")

    wsRequest.get().flatMap { response =>
      response.status match {
        case 200 => {
          try {
            Future.successful(response)
          } catch {
            case e: Exception => log.error(s"Unable to preview $urlIn (${e.getMessage})", e)
              Future.failed(new RuntimeException(s"Unable to preview $urlIn (${e.getMessage})", e))
          }
        }
        case non200 => {
          log.error(s"Unexpected response from ${wsRequest.uri}, status code: $non200")
          Future.failed(new RuntimeException(s"Unexpected response from ${wsRequest.uri}, status code: $non200"))
        }
      }
    }
  }

}

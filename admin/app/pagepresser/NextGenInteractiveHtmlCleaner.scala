package pagepresser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Play.current
import play.api.libs.ws.WS
import services.S3ArchiveOriginals

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

    val ngPreviewDoc = loadNextGenPreview(previewUrl)

    ngPreviewDoc.map{ previewResponse =>
      println("### RE-WRITING...")

      val ngTemplateDocUrl = "www.theguardian.com/template/ng-interactive-template.html"

      val newDoc = S3ArchiveOriginals.get(ngTemplateDocUrl).map { template =>
        val ngOutputTemplateDoc = Jsoup.parse(template)
        val srcDoc = document.clone()
        val previewDoc = Jsoup.parse(previewResponse.body)

        document.head().replaceWith(ngOutputTemplateDoc.head())
        document.body().replaceWith(ngOutputTemplateDoc.body())
        document.title(srcDoc.title())
        moveMeta(srcDoc, document)

        val interactiveElement = "content__main-column--interactive"

        previewDoc.getElementsByTag("div").filter(_.className().contains(interactiveElement)).foreach{ el =>
          document.getElementsByTag("div").filter(_.className().contains(interactiveElement)).foreach(_.replaceWith(el))
        }

        document

      }.getOrElse(document)

      // global replace all urls with https:
      val securedDoc = Jsoup.parse(secureSource(newDoc.html()))

      document.head().replaceWith(securedDoc.head())
      document.body().replaceWith(securedDoc.body())

      addJqueryScript(document)
      //    addRequireJsScript(document)
      universalClean(document)
      removeByTagName(document, "noscript")
      removeInsecureScripts(document)

      println("### RE-WRITTEN!")
      document
    }
  }

  private def secureSource(src: String): String = {
    src.replaceAllLiterally(""""//""", """"https://""").replaceAllLiterally("'//", "'https://").replaceAllLiterally("http://", "https://")
  }

  private def moveMeta(fromDoc: Document,
                       toDoc: Document): Document = {
    toDoc.getElementsByTag("meta").foreach(_.remove)
    fromDoc.getElementsByTag("meta").foreach { el =>
      val newMeta = toDoc.head().prependElement("meta")
      for (attr <- el.attributes()) {
        newMeta.attr(attr.getKey, secureSource(attr.getValue))
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

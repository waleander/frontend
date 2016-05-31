package pagepresser

import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
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
    //rewriteTemplate(document)
    universalClean(document)
    val tmpDoc = Jsoup.parse(secureSource(document.html()))
    document.head().replaceWith(tmpDoc.head())
    document.body().replaceWith(tmpDoc.body())
    document
  }

  private def rewriteTemplate(document: Document): Document = {
    println("### RE-WRITING...")

    val ngTemplateDocUrl = "www.theguardian.com/template/ng-interactive-template.html"

    val newDoc = S3ArchiveOriginals.get(ngTemplateDocUrl).map { template =>
      val ngOutputTemplateDoc = Jsoup.parse(template)
      val srcDoc = document.clone()

      document.head().replaceWith(ngOutputTemplateDoc.head())
      document.body().replaceWith(ngOutputTemplateDoc.body())
      document.title(srcDoc.title())
      moveMeta(srcDoc, document)

      val interactiveElement = "content__main-column--interactive"

      val interactiveBootPath = "https://interactive.guim.co.uk/next-gen" +
        srcDoc.getElementsByAttributeValue("rel","canonical")
        .attr("href")
        .toLowerCase
        .replaceAll("http://","")
        .replaceAll("https://","")
        .replaceAll("www.","")
        .replaceAll("theguardian.com","")
        .concat("/boot.js")

      document.getElementsByTag("figure").filter(_.parent().className().contains(interactiveElement)).foreach{ fig =>
        fig.attributes().foreach{ att =>
          att.getKey() match {
            case key if key == "data-interactive" || key == "data-canonical-url" => att.setValue(interactiveBootPath)
            case key if key == "data-alt" => att.setValue("Interactive content")
            case _ =>
          }
        }
      }

      println("### RE-WRITTEN")

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

}

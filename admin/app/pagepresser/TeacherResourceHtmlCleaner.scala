package pagepresser

import org.jsoup.nodes.Document
import scala.collection.JavaConversions._

object TeacherResourceHtmlCleaner extends HtmlCleaner {

  override def canClean(document: Document) = {
    document.getElementById("hdnPage") != null
  }

  override def clean(document: Document, convertToHttps: Boolean) = {
    universalClean(document)
    removeScripts(document)
    createSimplePageTracking(document)
    removeByTagName(document, "noscript")
    if (convertToHttps) secureDocument(document)
    document
  }

  override protected def universalClean(document: Document): Document = {
    removeAds(document)
    removeByTagWithAttribute(document, "li", "onclick")
    replaceLinks(document)
  }

  private def removeByTagWithAttribute(document: Document, tagName: String, attributeName: String): Document = {
    val els = document.getElementsByTag(tagName).foreach { el =>
      if (el.hasAttr(attributeName)) el.remove()
    }
    document
  }

}

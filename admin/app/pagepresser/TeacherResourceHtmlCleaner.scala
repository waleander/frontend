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
    removeByTagName(document, "noscript")
    if (convertToHttps) secureDocument(document)
    document
  }

  override protected def universalClean(document: Document): Document = {
    removeAds(document)
    replaceDownloadLink(document)
    removeByTagWithNestedElementAttribute(document, "li", "onclick")
    removeByTagWithNestedElementAttributeValueContains(document, "li", "href", ".aspx")
    replaceLinks(document)
    removeHiddenElements(document)
  }

  override def removeAds(document: Document): Document = {
    // TODO? (Might not want to, but if we do it'll be a different implementation than HtmlCleaner)
    document
  }

  private def removeByTagWithNestedElementAttribute(document: Document, tagName: String, nestedAttributeName: String): Document = {
    document.getElementsByTag(tagName).foreach { el =>
      if (el.getElementsByAttribute(nestedAttributeName).nonEmpty) el.remove()
    }
    document
  }

  private def removeByTagWithNestedElementAttributeValueContains(document: Document, tagName: String, nestedAttributeName: String, nestedAttributeValue: String): Document = {
    document.getElementsByTag(tagName).foreach { el =>
      if (el.getElementsByAttribute(nestedAttributeName).exists(_.attr(nestedAttributeName).contains(nestedAttributeValue))) {
        el.remove()
      }
    }
    document
  }

  private def replaceDownloadLink(document: Document): Document = {
    val fileNameEl = document.getElementById("hdnFilename")
    val downloadEl = document.getElementById("dwnspan")
    if (fileNameEl != null && downloadEl != null) {
      val fileName = fileNameEl.attr("value")
      if (fileName != null) {
        document.getElementById("dwnspan").parent().append(s"""<div><a href="$fileName" id="lnkbtnDownload" class="download_button hrefhover">Download</a></div>""")
      } else {
        log.error("### fileName is null")
      }
    } else {
      log.error("### download link and/or filename not present")
    }
    document
  }

  private def removeHiddenElements(document: Document): Document = {
    document.getElementsByAttributeValue("type", "hidden").foreach(_.remove())
    document
  }

  override def replaceLinks(document: Document): Document = {
    try {
      document.getAllElements.filter{ el =>
        (el.hasAttr("href") && el.attr("href").contains("http://")) || (el.hasAttr("src") && el.attr("src").contains("http://"))
      }.foreach{ el =>
        if (el.hasAttr("href")) {
          el.attr("href", el.attr("href").replace("http://", "//").replace("guardian.co.uk", "theguardian.com"))
        } else {
          el.attr("src", el.attr("src").replace("http://", "//").replace("guardian.co.uk", "theguardian.com"))
        }
      }
      document
    }
    catch {
      case e: Exception => {
        log.warn("Unable to convert links for document from http to protocol relative url.")
        document
      }
    }
  }

}

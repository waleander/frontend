package pagepresser

import org.jsoup.nodes.Document
import scala.collection.JavaConversions._

object TeacherResourceHtmlCleaner extends HtmlCleaner {

  override def canClean(document: Document) = {
    document.getElementById("hdnPage") != null
  }

  override def clean(document: Document, convertToHttps: Boolean) = {
    removeAds(document)
    replaceCssLinks(document)
    replaceResourceNameLink(document)
    replaceDownloadLink(document)
    removeAllTagsWithAttribute(document, "li", "onclick")
    removeAllTagsWithAttribute(document, "a", "onclick")
    removeAllTagsWithNestedElementAttributeValueContains(document, "li", "href", ".aspx")
    replaceLinks(document)
    removeHiddenElements(document)
    removeSpecificElements(document)
    replaceBackLink(document)
    removeScripts(document)
    removeByTagName(document, "noscript")
    if (convertToHttps) secureDocument(document)
    document
  }

  override def removeAds(document: Document): Document = {
    // TODO? (Might not want to, but if we do it'll be a different implementation than HtmlCleaner)
    document
  }

  private def replaceCssLinks(document: Document): Document = {
    document.getAllElements.filter { el =>
      el.hasAttr("href") && !el.attr("href").startsWith("http") && el.attr("href").endsWith(".css")
    }.foreach { el =>
      val cssRef = el.attr("href").split("/").last
      el.attr("href", s"/teachers.theguardian.com/CSS/$cssRef")
    }
    document
  }

  private def removeAllTagsWithAttribute(document: Document, tagName: String, attributeName: String): Document = {
    document.getElementsByTag(tagName).foreach { el =>
      if (el.hasAttr(attributeName)) el.remove()
    }
    document
  }

  private def removeAllTagsWithNestedElementAttributeValueContains(document: Document, tagName: String, nestedAttributeName: String, nestedAttributeValue: String): Document = {
    document.getElementsByTag(tagName).foreach { el =>
      if (el.getElementsByAttribute(nestedAttributeName).exists(_.attr(nestedAttributeName).contains(nestedAttributeValue))) {
        el.remove()
      }
    }
    document
  }

  private def replaceResourceNameLink(document: Document): Document = {
    val fileName = document.getElementById("hdnFilename").attr("value")
    val downloadNameLinkEl = document.getElementById("previewlesson").clone()
    val fileDescription = downloadNameLinkEl.getElementById("lblLname").text()
    downloadNameLinkEl.removeAttr("onclick")
    downloadNameLinkEl.attr("href", fileName)
    document.getElementById("previewlesson").replaceWith(downloadNameLinkEl)
    document
  }

  private def replaceDownloadLink(document: Document): Document = {
    val fileNameEl = document.getElementById("hdnFilename")
    val downloadEl = document.getElementById("dwnspan").clone()
    if (fileNameEl != null && downloadEl != null) {
      val fileName = fileNameEl.attr("value")
      if (fileName != null) {
        val newLinkHtml = s"""<a href="$fileName" id="lnkbtnDownload" class="download_button hrefhover">Download</a>"""
        downloadEl.getElementsByTag("a").foreach(_.remove())
        downloadEl.append(newLinkHtml)
        document.getElementById("dwnspan").replaceWith(downloadEl)
      } else {
        log.error("fileName is null")
      }
    } else {
      log.error("download link and/or filename not present")
    }
    document
  }

  private def replaceBackLink(document: Document): Document = {
    document.getElementById("lnkbtnBack").attr("href", "https://teachers.theguardian.com/teacher-resources")
    document
  }

  private def removeHiddenElements(document: Document): Document = {
    document.getElementsByAttributeValue("type", "hidden").foreach(_.remove())
    document
  }

  private def removeSpecificElements(document: Document): Document = {
    document.getElementById("Header1_Menu1_myresource").remove()
    document.getElementById("Header1_Menu1_searchresource").remove()
    document.getElementById("Header1_Menu1_testurclass").remove()
    document.getElementsByClass("lessonlinks_new").foreach(_.remove())
    document.getElementById("UpdatePanel1").remove()
    document.getElementById("usppaging1").remove()
    document.getElementById("Confirm_modalOverlay").remove()
    document.getElementById("Confirm_modalHolder").remove()
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

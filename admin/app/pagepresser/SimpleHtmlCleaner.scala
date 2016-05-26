package pagepresser

import org.jsoup.nodes.Document

import scala.concurrent.Future

object SimpleHtmlCleaner extends HtmlCleaner {

  override def canClean(document: Document) = {
    document.getElementsByAttribute("data-poll-url").isEmpty &&
    document.getElementById("interactive-content") == null
  }

  override def clean(document: Document) = Future {
    universalClean(document)
    removeScripts(document)
    createSimplePageTracking(document)
    removeByTagName(document, "noscript")
  }
}

package pagepresser

import com.netaporter.uri.Uri._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import services.S3ArchiveOriginals
import scala.collection.JavaConversions._
import scala.io.Source

object InteractiveHtmlCleaner extends HtmlCleaner with implicits.WSRequests {

  override def canClean(document: Document): Boolean = {
    document.getElementById("interactive-content") != null
  }

  override def clean(document: Document) = {
    val docPath = document.getElementsByAttributeValue("rel","canonical").attr("href").toLowerCase
    if (docPath.contains("/ng-interactive/")) {
      rewriteTemplate(document)
    } else {
      universalClean(document)
      removeScripts(document)
      removeByTagName(document, "noscript")
      createSimplePageTracking(document)
    }

  }

  override def extractOmnitureParams(document: Document) = {
    val omnitureNoScript = if(document.getElementById("omnitureNoScript") != null) {
      document.getElementById("omnitureNoScript")
    } else {
      // Dear reviewer, this horrible code is here because I've found examples of interactives that have nested iframes.
      // Yes, honestly. Nested iframes. Which Jsoup struggles to parse properly.
      // Anyway, the omnitureNoScript element we're seeking may exist at some nested level and so this is an attempt at
      // working arounf the shortcomings of Jsoup's parser. It's horrible and I'm sorry. But I need to get on with
      // pressing these damned things. If you know a better way, please say.
      document.getElementsByTag("iframe").map { frame =>
        val tempDoc: Element = {
          val x = Jsoup.parseBodyFragment(frame.html()).getElementById("omnitureNoScript")
          if (x != null) {
            log.info("omnitureNoScript matched at level 1")
            x
          } else {
            val y = Jsoup.parseBodyFragment(Jsoup.parseBodyFragment(frame.html()).text()).getElementById("omnitureNoScript")
            if ( y != null) {
              log.info("omnitureNoScript matched at level 2")
              y
            } else {
              val z = Jsoup.parseBodyFragment(Jsoup.parseBodyFragment(Jsoup.parse(frame.html()).text()).text()).getElementById("omnitureNoScript")
              if (z != null) {
                log.info("omnitureNoScript matched at level 3")
                z
              } else {
                log.error("no omnitureNoScript element found")
                null
              }
            }
          }
        }
        tempDoc
      }.map(_.getElementById("omnitureNoScript")).headOption.orNull
    }

    val params: Map[String, Seq[String]] = if (omnitureNoScript != null) {
      parse(omnitureNoScript.getElementsByTag("img").attr("src")).query.paramMap
    } else {
      log.error("Failed to extract params from omnitureNoScript (element cannot be found)")
      Map.empty
    }
    params
  }

  override def removeScripts(document: Document): Document = {
    val scripts = document.getElementsByTag("script")
    val needsJquery = scripts.exists(_.html().toLowerCase.contains("jquery"))

    val (interactiveScripts, nonInteractiveScripts) = scripts.partition { e =>
      val parentIds = e.parents().map(p => p.id()).toList
      parentIds.contains("interactive-content")
    }
    nonInteractiveScripts.toList.foreach(_.remove())

    interactiveScripts.toList.map { interactiveElement =>
      if (interactiveElement.html().contains("swfobject")) {
        addSwfObjectScript(document)
      }
    }

    if (needsJquery) {
      addJqueryScript(document)
    }

    document
  }

  private def rewriteTemplate(document: Document): Document = {
    val docPath = document.getElementsByAttributeValue("rel","canonical").attr("href").toLowerCase
    if (!docPath.contains("/ng-interactive/")) {
      document
    } else {
      println(s" ### ${this.getClass.getCanonicalName} RE-WRITING...")
      S3ArchiveOriginals.get("www.theguardian.com/template/ng-interactive-template.html").map { template =>
        val srcDoc = document.clone()
        val templateDoc = Jsoup.parse(template)

        document.head().replaceWith(templateDoc.head())
        document.body().replaceWith(templateDoc.body())

        document.title(srcDoc.title())

        moveMeta(srcDoc, document)
        moveHeadElements(srcDoc, "link", "rel", "canonical", document)
        moveHeadElements(srcDoc, "link", "rel", "shorturl", document)
        moveHeadElements(srcDoc, "link", "rel", "publisher", document)
        //moveHeadElements(srcDoc, "link", "rel", "stylesheet", oldDoc)

        val interactiveHost = "https://interactive.guim.co.uk/next-gen"
        val interactivePath =  docPath
          .stripPrefix("http:")
          .stripPrefix("https:")
          .stripPrefix("//")
          .stripPrefix("www.")
          .stripPrefix("theguardian.com")
          .stripPrefix("theguardian.co.uk")
          .concat("/boot.js")

        val newInteractiveAttrs = Map(
          ("data-interactive", interactiveHost + interactivePath),
          ("data-canonical-url", interactiveHost + interactivePath),
          ("data-alt", "Interactive content"))

        rewriteElement(document, "figure", "class", "element element-interactive interactive", newInteractiveAttrs)

        for (el <- document.getAllElements) {
          if ((el.hasAttr("src") && el.attr("src").startsWith("http://")) || (el.hasAttr("href") && el.attr("href").startsWith("http://"))) {
            if(el.hasAttr("src")) {
              el.attr("src", el.attr("src").replaceAll("http://","https://"))
            } else {
              el.attr("href", el.attr("href").replaceAll("http://","https://"))
            }
          }
        }

        println("### RE-WRITTEN!")
        document

      }.getOrElse(document)
    }
  }

  private def moveMeta(fromDoc: Document,
                       toDoc: Document): Document = {
    toDoc.getElementsByTag("meta").foreach(_.remove)
    fromDoc.getElementsByTag("meta").foreach { el =>
      val newMeta = toDoc.head().prependElement("meta")
      for (attr <- el.attributes()) {
        newMeta.attr(attr.getKey, attr.getValue)
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

    toDoc.getElementsByTag(tag).filter(el => el.hasAttr(attributeKey) && el.attr(attributeKey) == attributeVal).foreach(_.remove)

    fromDoc.getElementsByAttributeValue(attributeKey, attributeVal).foreach{ el =>
      val newEl = toDoc.head().prependElement(tag)
      for (attr <- el.attributes()) {
        newEl.attr(attr.getKey, attr.getValue)
      }
    }
    toDoc
  }

  private def rewriteElement(document: Document,
                             tag: String,
                             attributeKey: String,
                             attributeValue: String,
                             newAttributes: Map[String,String] = Map.empty,
                             newHtml: String = ""): Document = {
    document.getElementsByTag(tag).filter(el => el.hasAttr(attributeKey) && el.attr(attributeKey) == attributeValue).foreach{ el =>
      println(s"### el: ${el.cssSelector()}")
      if (newAttributes.nonEmpty) {
        newAttributes.foreach { attr =>
          println(s"### attr: ${attr._1} val: ${attr._2}")
          el.removeAttr(attr._1)
          el.attr(attr._1, attr._2)
        }
      }
      el.html(newHtml)
    }
    document
  }

  private def addJqueryScript(document: Document): Document = {
    val jqScript = """
    <script src="//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min.js"></script>
    <script>
    var jQ = jQuery.noConflict();
    jQ.ajaxSetup({ cache: true });
  </script>"""
    document.body().prepend(jqScript)
    document
  }

  private def addSwfObjectScript(document: Document): Document = {
    val swfScriptOpt = try {
      val source = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("resources/r2/interactiveSwfScript.js"), "UTF-8").getLines().mkString
      Some(source)
    } catch {
      case ex: Exception => {
        log.error(ex.getMessage)
        None
      }
    }
    swfScriptOpt.foreach { script =>
      val html = "<script type=\"text/javascript\">" + script + "</script>"
      document.head().append(html)
    }
    document
  }

}

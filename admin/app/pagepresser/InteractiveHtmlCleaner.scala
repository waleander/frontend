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
      addJqueryScript(document)
      universalClean(document)
    } else {
      universalClean(document)
      removeScripts(document)
      addJqueryScript(document)
      addRequireJsScript(document)
      createSimplePageTracking(document)
    }
    removeByTagName(document, "noscript")
    removeInsecureScripts(document)
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
    log.info("Removing scripts")
    val scripts = document.getElementsByTag("script")

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

    document
  }

  private def rewriteTemplate(document: Document): Document = {
    val docPath = document.getElementsByAttributeValue("rel","canonical").attr("href").toLowerCase
    if (!docPath.contains("/ng-interactive/")) {
      document
    } else {
      println(s"### ${this.getClass.getCanonicalName} RE-WRITING...")
      val templateSource = "www.theguardian.com/template/view-source_balham-then-and-now.html"
      S3ArchiveOriginals.get(templateSource).map { template =>
        val srcDoc = document.clone()
        val templateDoc = Jsoup.parse(template)

        document.head().replaceWith(templateDoc.head())
        document.body().replaceWith(templateDoc.body())

        document.title(srcDoc.title())

        moveMeta(srcDoc, document)
        moveHeadElements(srcDoc, "link", "rel", "canonical", document)
        moveHeadElements(srcDoc, "link", "rel", "shorturl", document)
        moveHeadElements(srcDoc, "link", "rel", "publisher", document)
        moveHeadElements(srcDoc, "link", "rel", "stylesheet", document)

        document.getElementsByAttributeValue("class", "content__main-column content__main-column--interactive").foreach { interactiveElement =>
          interactiveElement.getElementsByTag("figure").foreach(_.remove())
          getChildElementsOf(srcDoc, "div", "id", "interactive").foreach { el =>
            if (el.tagName() == "figure") {
              if (!el.classNames().exists(_.equalsIgnoreCase("element"))) {
                el.addClass("element")
              }
              if (!el.classNames().exists(_.equalsIgnoreCase("element-interactive"))) {
                el.addClass("element-interactive")
              }
              if (!el.classNames().exists(_.equalsIgnoreCase("interactive"))) {
                el.addClass("interactive")
              }
              if (!el.attributes().exists(_.getKey.toLowerCase == "data-alt")) {
                el.attr("data-alt","Interactive content")
              }
            }
            interactiveElement.appendChild(el)
          }
        }

//        val interactiveHost = "https://interactive.guim.co.uk/next-gen"
//        val interactivePath =  docPath
//          .stripPrefix("http:")
//          .stripPrefix("https:")
//          .stripPrefix("//")
//          .stripPrefix("www.")
//          .stripPrefix("theguardian.com")
//          .stripPrefix("theguardian.co.uk")
//          .concat("/boot.js")
//
//        val newInteractiveAttrs = Map(
//          ("data-interactive", interactiveHost + interactivePath),
//          ("data-canonical-url", interactiveHost + interactivePath),
//          ("data-alt", "Interactive content"))
//
//        rewriteElement(document, "figure", "class", "element element-interactive interactive", newInteractiveAttrs)

        // global replace http: with protocol-neutral
        val securedDoc = Jsoup.parse(securedSource(document.html()))
        document.head().replaceWith(securedDoc.head())
        document.body().replaceWith(securedDoc.body())

        println("### RE-WRITTEN!")
        document

      }.getOrElse(document)
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

//  private def rewriteElement(document: Document,
//                             tag: String,
//                             attributeKey: String,
//                             attributeValue: String,
//                             newAttributes: Map[String,String] = Map.empty,
//                             newHtml: String = ""): Document = {
//    document.getElementsByTag(tag).filter(el => el.hasAttr(attributeKey) && el.attr(attributeKey) == attributeValue).foreach{ el =>
//      println(s"### el: ${el.cssSelector()}")
//      if (newAttributes.nonEmpty) {
//        newAttributes.foreach { attr =>
//          println(s"### attr: ${attr._1} val: ${attr._2}")
//          el.removeAttr(attr._1)
//          el.attr(attr._1, attr._2)
//        }
//      }
//      el.html(newHtml)
//    }
//    document
//  }

  private def removeInsecureScripts(document: Document): Document = {
    document.getElementsByTag("script").foreach{ scriptEl =>
      if (scriptEl.hasAttr("src") && scriptEl.attr("src").startsWith("http:")){
        log.info(s"Remove insecure script: src ${scriptEl.attr("src")}")
        scriptEl.remove()
      }
    }
    document
  }

  private def addJqueryScript(document: Document): Document = {
    log.info("Adding JQuery script")

    val jqScript = """
    <script type="text/javascript" charset="utf-8" src="https://pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min.js"></script>
    <script type="text/javascript">
    var jQ = jQuery.noConflict();
    jQ.ajaxSetup({ cache: true });
  </script>"""
    document.head().prepend(jqScript)
    document
  }

  private def addRequireJsScript(document: Document): Document = {
    log.info("Adding RequireJS")
    val rqScript = """
    <script type="text/javascript" charset="utf-8" src="https://pasteup.guim.co.uk/js/lib/requirejs/2.1.5/require.min.js"
          data-main="https://static.guim.co.uk/static/6d5811c93d9b815024b5a6c3ec93a54be18e52f0/common/scripts/main.js"
          data-modules="gu/author-twitter-handles"
          data-callback=""
          id="require-js">
    </script>""".stripMargin
    document.head().prepend(rqScript)
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

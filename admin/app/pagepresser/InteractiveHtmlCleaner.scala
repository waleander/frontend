package pagepresser

import com.netaporter.uri.Uri._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.Play.current
import play.api.libs.ws.WS

import scala.collection.JavaConversions._
import scala.io.Source

object InteractiveHtmlCleaner extends HtmlCleaner with implicits.WSRequests {

  override def canClean(document: Document): Boolean = {
    document.getElementById("interactive-content") != null
  }

  override def clean(document: Document) = {
    universalClean(document)
    removeScripts(document)
    createSimplePageTracking(document)
    removeByTagName(document, "noscript")
    // TODO: May not need to do this
    genericiseInteractiveOutboundRefs(document),
    inlineAllGiaScripts(document)
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

  private def inlineAllGiaScripts(document: Document): Document = {
    for (el <- document.getElementsByAttributeValue("id","js-gia")) {
      val src = "http://" + el.attr("src").replace("https:","").replace("http:","").replace("//","")
      for (response <- WS.url(src).get()) {
        val newSrc = response.body.replaceAll("http:", "https:")
        println(s"prepend ${newSrc.take(20)}")
        el.parent().prepend(newSrc)
        println(s"remove $src")
        el.remove()
      }
    }
    //TO DO: The above is async so returning document like this is dumb. Fix it.
    document
  }

  // TODO: May not need to do this
  private def genericiseInteractiveOutboundRefs(document: Document): Document = {
    val interactiveContainer = document.getElementById("interactive-content")
    secureElement(interactiveContainer, 1)
    document
  }

  // TODO: May not need to do this
  private def secureElement(element: Element, level: Int): Element = {
    println(s"### secureElement (level$level): ${element.cssSelector()}")
    if (element.children().nonEmpty) {
      element.children().map(el => secureElement(el,level + 1))
    }

    println(s"### ${element.cssSelector()}")
    for (attr <- element.attributes) {
      println(attr.toString)
    }

    if ((element.hasAttr("href") && element.attr("href").contains("http://")) || (element.hasAttr("src") && element.attr("src").contains("http://"))) {
      println(s"### element IN ${element.id()}\n${element.html()}")
      if (element.hasAttr("href")) {
        element.attr("href", element.attr("href").replace("http://", "https://"))
      } else {
        element.attr("src", element.attr("src").replace("http://", "https://"))
      }
      println("###")
      println(s"### element OUT:  ${element.id()}\n${element.html()}")
      println("###")
    }
    element
  }

}

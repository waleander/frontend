package controllers

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.model.v1.ContentType.{Article => CapiArticle}
import com.gu.contentapi.json.JsonParser
import com.gu.facia.api.models.{FaciaContent, FaciaImage, Groups, CuratedContent => FapiCuratedContent}
import com.gu.facia.api.utils._
import com.gu.facia.client.models.{CollectionConfigJson, TrailMetaData}
import common._
import controllers.front._
import layout.{CollectionEssentials, FaciaContainer, Front}
import model.Cached.{RevalidatableResult, WithoutRevalidationResult}
import model._
import model.facia.PressedCollection
import model.pressed.{CollectionConfig, CuratedContent}
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html
import services.{CollectionConfigWithId, ConfigAgent}
import slices._
import views.html.fragments.containers.facia_cards.container
import views.support.FaciaToMicroFormat2Helpers.getCollection
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful
import play.api.Play.current

trait FaciaController extends Controller with Logging with ExecutionContexts with implicits.Collections with implicits.Requests {

  val EditionalisedKey = """^\w\w(/.*)?$""".r

  val frontJsonFapi: FrontJsonFapi

  private def getEditionFromString(edition: String) = {
    val editionToFilterBy = edition match {
      case "international" => "int"
      case _ => edition
    }
    Edition.all.find(_.id.toLowerCase() == editionToFilterBy).getOrElse(Edition.all.head)
  }

  def applicationsRedirect(path: String)(implicit request: RequestHeader) = {
    successful(InternalRedirect.internalRedirect("applications", path, request.rawQueryStringOption.map("?" + _)))
  }

  def rssRedirect(path: String)(implicit request: RequestHeader) = {
    successful(InternalRedirect.internalRedirect(
      "rss_server",
      path,
      request.rawQueryStringOption.map("?" + _)
    ))
  }

  //Only used by dev-build for rending special urls such as lifeandstyle/home-and-garden
  def renderFrontPressSpecial(path: String) = Action.async { implicit request => renderFrontPressResult(path) }

  // Needed as aliases for reverse routing
  def renderFrontJson(id: String) = renderFront(id)

  def renderContainerJson(id: String) = renderContainer(id, false)

  def renderSomeFrontContainers(path: String, rawNum: String, rawOffset: String, sectionNameToFilter: String, edition: String) = Action.async { implicit request =>

    def returnContainers(num: Int, offset: Int) = getSomeCollections(Editionalise(path, getEditionFromString(edition)), num, offset, sectionNameToFilter).map { collections =>

        val containers = collections.getOrElse(List()).zipWithIndex.map { case (collection: PressedCollection, index) =>

          val containerLayout = if(collection.collectionType.contains("mpu")) {
              Fixed(FixedContainers.frontsOnArticles)
            } else {
              Container.resolve(collection.collectionType)
            }

          val containerDefinition = FaciaContainer(
            index,
            containerLayout,
            CollectionConfigWithId("", CollectionConfig.empty),
            CollectionEssentials.fromPressedCollection(collection).copy(treats = Nil)
          )

          container(containerDefinition, FrontProperties.empty)
        }

        if(request.isJson) {
          Cached(60) {JsonCollection(Html(containers.mkString))}
        } else {
          Cached(60)(WithoutRevalidationResult(NotFound))
        }
    }

    (rawNum, rawOffset) match {
      case (Int(num), Int(offset)) => returnContainers(num, offset)
      case _ => Future.successful(Cached(600) {
        WithoutRevalidationResult(BadRequest)
      })
    }
  }

  def renderSomeFrontContainersMf2(count: Int, offset: Int, section: String = "", edition: String = "") = Action.async { implicit request =>
    val e = if(edition.isEmpty) Edition(request) else getEditionFromString(edition)
    val collectionsPath = if(section.isEmpty) e.id.toLowerCase else Editionalise(section, e)
    getSomeCollections(collectionsPath, count, offset, "none").map { collections =>
      Cached(60) {
        JsonComponent(
          "items" -> JsArray(collections.getOrElse(List()).map(getCollection))
        )
      }
    }

  }

  def renderContainerJsonWithFrontsLayout(id: String) = renderContainer(id, true)

  // Needed as aliases for reverse routing
  def renderRootFrontRss() = renderFrontRss(path = "")
  def renderFrontRss(path: String) = Action.async { implicit  request =>
    log.info(s"Serving RSS Path: $path")
    if (shouldEditionRedirect(path))
      redirectTo(s"${Editionalise(path, Edition(request))}/rss")
    else if (!ConfigAgent.shouldServeFront(path))
      rssRedirect(s"$path/rss")
    else
      renderFrontPressResult(path)
  }

  def rootEditionRedirect() = renderFront(path = "")
  def renderFront(path: String) = Action.async { implicit request =>
    log.info(s"Serving Path: $path")
    if (path == "search-hackday")
      renderSearchFront()
    else if (shouldEditionRedirect(path))
      redirectTo(Editionalise(path, Edition(request)))
    else if (!ConfigAgent.shouldServeFront(path) || request.getQueryString("page").isDefined)
      applicationsRedirect(path)
    else
      renderFrontPressResult(path)
  }

  private def shouldEditionRedirect(path: String)(implicit request: RequestHeader) = {
    val editionalisedPath = Editionalise(path, Edition(request))
    (editionalisedPath != path) && request.getQueryString("page").isEmpty
  }

  def redirectTo(path: String)(implicit request: RequestHeader): Future[Result] = successful {
    val params = request.rawQueryStringOption.map(q => s"?$q").getOrElse("")
    Cached(60)(WithoutRevalidationResult(Found(LinkTo(s"/$path$params"))))
  }

  def renderFrontJsonLite(path: String) = Action.async { implicit request =>
    val cacheTime = 60
    frontJsonFapi.get(path).map {
        case Some(pressedPage) => Cached(cacheTime)(JsonComponent(FapiFrontJsonLite.get(pressedPage)))
        case None => Cached(cacheTime)(JsonComponent(JsObject(Nil)))}
  }

  private[controllers] def renderFrontPressResult(path: String)(implicit request: RequestHeader) = {
    val futureResult = frontJsonFapi.get(path).flatMap {
      case Some(faciaPage) =>
        successful(
          if (request.isRss) {
            val body = TrailsToRss.fromPressedPage(faciaPage)
            Cached(faciaPage) {
              RevalidatableResult(Ok(body).as("text/xml; charset=utf-8"), body)
            }
          }
          else if (request.isJson)
            Cached(faciaPage)(JsonFront(faciaPage))
          else {
            Cached(faciaPage) {
              RevalidatableResult.Ok(views.html.front(faciaPage))
            }
          }
        )
      case None => successful(Cached(60)(WithoutRevalidationResult(NotFound)))}

    futureResult.onFailure { case t: Throwable => log.error(s"Failed rendering $path with $t", t)}
    futureResult
  }

  private def mockPressedPage (collections: List[PressedCollection]) : PressedPage = {
    PressedPage(
      id= "search-hackday",
      seoData = SeoData(
        id = "search",
        navSection = "uk",
        webTitle = "Search",
        title = Some("Advanced search for hackday"),
        description = None
      ),
      frontProperties = FrontProperties(None, None, None, None, false, None, None),
      collections = collections
    )
  }

  private def mockCollection (
            contentList: List[CuratedContent],
            collectionConfig: com.gu.facia.api.models.CollectionConfig
  ): PressedCollection = {
    val listOfGroups = Some(List("0", "1", "2", "3"))
    PressedCollection(
      id = "search-result",
      displayName = collectionConfig.displayName.get,
      curated = contentList,
      backfill = Nil,
      treats = Nil,
      lastUpdated = None,
      updatedBy = None,
      updatedEmail = None,
      href = None,
      description = None,
      collectionType = collectionConfig.collectionType,
      groups = listOfGroups,
      uneditable = true,
      showTags = false,
      showSections = false,
      hideKickers = false,
      showDateHeader = false,
      showLatestUpdate = false,
      config = CollectionConfig.make(collectionConfig)
    )
  }

  private def mockContent(capiContent: Content, group: String, collectionConfig: com.gu.facia.api.models.CollectionConfig) : CuratedContent = {
    CuratedContent.make(
      FapiCuratedContent.fromTrailAndContent(
        content = capiContent,
        trailMetaData = TrailMetaData.withDefaults("group" -> JsString(group)),
        maybeFrontPublicationDate = None,
        collectionConfig = collectionConfig
      )
    )
  }

  private[controllers] def renderSearchFront()(implicit request: RequestHeader) = {
    val query = request.queryString.getOrElse("query", Nil)
    query match {
      case Seq(key) => searchFrontFromCapi(key)
      case _ => successful(Cached(60)(WithoutRevalidationResult(NotFound)))
    }
  }

  private def createCollectionConfig(displayName: String, cType: String, groups: List[String]): com.gu.facia.api.models.CollectionConfig = {
    com.gu.facia.api.models.CollectionConfig(
      displayName = Some(displayName),
      backfill = None,
      metadata = None,
      collectionType = cType,
      href = None,
      description = None,
      groups = Some(Groups(groups)),
      uneditable = false,
      showTags = false,
      showSections = false,
      hideKickers = false,
      showDateHeader = false,
      showLatestUpdate = false,
      excludeFromRss = true,
      showTimestamps = false,
      hideShowMore = false
    )
  }

  private def listOfCapiArticles (array: JsArray, collectionConfig: com.gu.facia.api.models.CollectionConfig): List[CuratedContent] = {
    val listOfContent = array.value.map {
      case thing: JsObject => {
        val content = JsonParser.parseContent(Json.stringify(thing))
        mockContent(content, "1", collectionConfig)
      }
      case _ => ???
    }
    listOfContent.toList
  }

  private def listFromLayout (layout: String): List[String] = {
    layout match {
      case "dynamic/slow" => List("0", "1", "2", "3")
      case _ => List("0")
    }
  }

  private def searchFrontFromCapi (key: String)(implicit request: RequestHeader) = {
    val later = WS.url(s"https://powerful-fortress-59898.herokuapp.com/content?q=$key").get().map { response =>
      println("respo", response.json)
      val listFromApi: Seq[PressedCollection] = response.json match {
        case array: JsArray => {
          array.value.flatMap {
            case collectionDescription: JsValue =>
              for {
                name <- (collectionDescription \ "name").asOpt[String]
                layout <- (collectionDescription \ "layout").asOpt[String]
                list <- (collectionDescription \ "results").asOpt[JsArray]
              } yield {
                val collectionConfig = createCollectionConfig(name, layout, listFromLayout(layout))
                val stories = listOfCapiArticles(list, collectionConfig)
                mockCollection(stories, collectionConfig)
              }
          }
        }
        case _ => Nil
      }

      Option(mockPressedPage(listFromApi.toList))
    }
    val futureResult = later.flatMap {
      case Some(faciaPage) =>
        successful(
          if (request.isRss) {
            val body = TrailsToRss.fromPressedPage(faciaPage)
            Cached(faciaPage) {
              RevalidatableResult(Ok(body).as("text/xml; charset=utf-8"), body)
            }
          }
          else if (request.isJson)
            Cached(faciaPage)(JsonFront(faciaPage))
          else {
            Cached(faciaPage) {
              RevalidatableResult.Ok(views.html.front(faciaPage))
            }
          }
        )
      case None => successful(Cached(60)(WithoutRevalidationResult(NotFound)))}

    futureResult.onFailure { case t: Throwable => log.error(s"Failed rendering search hackday with $t", t)}
    futureResult
  }

  def renderFrontPress(path: String) = Action.async { implicit request => renderFrontPressResult(path) }

  def renderContainer(id: String, preserveLayout: Boolean = false) = Action.async { implicit request =>
    log.info(s"Serving collection ID: $id")
    renderContainerView(id, preserveLayout)
  }

  def renderMostRelevantContainerJson(path: String) = Action.async { implicit request =>
    log.info(s"Serving most relevant container for $path")

    val canonicalId = ConfigAgent.getCanonicalIdForFront(path).orElse (
      alternativeEndpoints(path).map(ConfigAgent.getCanonicalIdForFront).headOption.flatten
    )

    canonicalId.map { collectionId =>
      renderContainerView(collectionId)
    }.getOrElse(successful(NotFound))
  }

  def alternativeEndpoints(path: String) = path.split("/").toList.take(2).reverse

  private def renderContainerView(collectionId: String, preserveLayout: Boolean = false)(implicit request: RequestHeader): Future[Result] = {
    log.info(s"Rendering container view for collection id $collectionId")
    getPressedCollection(collectionId).map { collectionOption =>
      collectionOption.map { collection =>

          val config = ConfigAgent.getConfig(collectionId).getOrElse(CollectionConfig.empty)

          val containerLayout = {
            if (preserveLayout)
              Container.resolve(collection.collectionType)
            else
              Fixed(FixedContainers.fixedSmallSlowVI)
          }

          val containerDefinition = FaciaContainer(
            1,
            containerLayout,
            CollectionConfigWithId(collectionId, config),
            CollectionEssentials.fromPressedCollection(collection)
          )

          val html = container(containerDefinition, FrontProperties.empty)
          if (request.isJson)
            Cached(60) {JsonCollection(html)}
          else
            Cached(60)(WithoutRevalidationResult(NotFound("containers are only available as json")))
      }.getOrElse(Cached(60)(WithoutRevalidationResult(NotFound(s"collection id $collectionId does not exist"))))
    }
  }

  def renderShowMore(path: String, collectionId: String) = Action.async { implicit request =>
    frontJsonFapi.get(path).flatMap {
      case Some(pressedPage) =>
        val containers = Front.fromPressedPage(pressedPage, Edition(request)).containers
        val maybeResponse =
          for {
            (container, index) <- containers.zipWithIndex.find(_._1.dataId == collectionId)
            containerLayout <- container.containerLayout}
          yield
            successful{Cached(pressedPage) {
            JsonComponent(views.html.fragments.containers.facia_cards.showMore(containerLayout.remainingCards, index))}}

        maybeResponse getOrElse successful(Cached(60)(WithoutRevalidationResult(NotFound)))
      case None => successful(Cached(60)(WithoutRevalidationResult(NotFound)))}}


  private object JsonCollection{
    def apply(html: Html)(implicit request: RequestHeader) = JsonComponent(
      "html" -> html
    )
  }

  private object JsonFront{
    def apply(faciaPage: PressedPage)(implicit request: RequestHeader) = JsonComponent(
      "html" -> views.html.fragments.frontBody(faciaPage),
      "config" -> Json.parse(templates.js.javaScriptConfig(faciaPage).body)
    )
  }

  private def getPressedCollection(collectionId: String): Future[Option[PressedCollection]] =
    ConfigAgent.getConfigsUsingCollectionId(collectionId).headOption.map { path =>
      frontJsonFapi.get(path).map(_.flatMap{ faciaPage =>
        faciaPage.collections.find{ c => c.id == collectionId}
      })
    }.getOrElse(successful(None))

  private def getSomeCollections(path: String, num: Int, offset: Int = 0, containerNameToFilter: String): Future[Option[List[PressedCollection]]] =
      frontJsonFapi.get(path).map(_.flatMap{ faciaPage =>
        // To-do: change the filter to only exclude thrashers and empty collections, not items such as the big picture
        Some(faciaPage.collections.filterNot(collection => (collection.curated ++ collection.backfill).length < 2 || collection.displayName == "most popular" || collection.displayName.toLowerCase.contains(containerNameToFilter.toLowerCase)).drop(offset).take(num))
      })

  /* Google news hits this endpoint */
  def renderCollectionRss(id: String) = Action.async { implicit request =>
    log.info(s"Serving collection ID: $id")
    getPressedCollection(id).flatMap {
      case Some(collection) =>
        successful{
          Cached(60) {
            val config: CollectionConfig = ConfigAgent.getConfig(id).getOrElse(CollectionConfig.empty)
            val webTitle = config.displayName.getOrElse("The Guardian")
            val body = TrailsToRss.fromFaciaContent(webTitle, collection.curatedPlusBackfillDeduplicated.flatMap(_.properties.maybeContent), "", None)
            RevalidatableResult(Ok(body).as("text/xml; charset=utf8"), body)
          }
        }
      case None => successful(Cached(60)(WithoutRevalidationResult(NotFound)))}
  }


  def renderAgentContents = Action {
    Ok(ConfigAgent.contentsAsJsonString)
  }
}

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}

object FaciaController extends FaciaController {
  val frontJsonFapi: FrontJsonFapi = FrontJsonFapiLive
}

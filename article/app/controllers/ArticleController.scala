package controllers

import _root_.liveblog.LiveBlogCurrentPage
import com.gu.contentapi.client.model.v1.{ItemResponse, Content => ApiContent}
import common._
import conf.switches.Switches
import contentapi.ContentApiClient
import model.Cached.WithoutRevalidationResult
import model._
import model.liveblog.{BodyBlock, KeyEventData}
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import play.api.mvc._
import views.support._

import scala.concurrent.Future
import scala.util.parsing.combinator.RegexParsers

trait PageWithStoryPackage extends ContentPage {
  def article: Article
  def related: RelatedContent
  override lazy val item = article
}

case class ArticlePage(article: Article, related: RelatedContent) extends PageWithStoryPackage
case class LiveBlogPage(article: Article, currentPage: LiveBlogCurrentPage, related: RelatedContent) extends PageWithStoryPackage
case class MinutePage(article: Article, related: RelatedContent) extends PageWithStoryPackage

class ArticleController extends Controller with RendersItemResponse with Logging with ExecutionContexts {

  private def isSupported(c: ApiContent) = c.isArticle || c.isLiveBlog || c.isSudoku
  override def canRender(i: ItemResponse): Boolean = i.content.exists(isSupported)
  override def renderItem(path: String)(implicit request: RequestHeader): Future[Result] = mapModel(path, blocks = true)(render(path, _))


  private def renderNewerUpdates(page: LiveBlogPage, lastUpdateBlockId: String)(implicit request: RequestHeader): Result = {
    val blocksHtml = views.html.liveblog.liveBlogBlocks(page.currentPage.currentPage.blocks, page.article, Edition(request).timezone)
    val timelineHtml = views.html.liveblog.keyEvents("", KeyEventData(page.currentPage.currentPage.blocks, Edition(request).timezone))
    val allPagesJson = Seq(
      "timeline" -> timelineHtml,
      "numNewBlocks" -> page.currentPage.currentPage.blocks.size,
      "html" -> blocksHtml
    )
    val mostRecent = page.article.fields.blocks.headOption.map { block =>
      "mostRecentBlockId" -> s"block-${block.id}"
    }
    Cached(page)(JsonComponent((allPagesJson ++ mostRecent): _*))
  }

  case class TextBlock(
    id: String,
    title: Option[String],
    publishedDateTime: Option[DateTime],
    lastUpdatedDateTime: Option[DateTime],
    body: String
    )

  implicit val blockWrites = (
    (__ \ "id").write[String] ~
      (__ \ "title").write[Option[String]] ~
      (__ \ "publishedDateTime").write[Option[DateTime]] ~
      (__ \ "lastUpdatedDateTime").write[Option[DateTime]] ~
      (__ \ "body").write[String]
    )(unlift(TextBlock.unapply))

  private def blockText(page: PageWithStoryPackage, number: Int)(implicit request: RequestHeader): Result = page match {
    case LiveBlogPage(liveBlog, _, _) =>
      val blocks = liveBlog.blocks.collect {
        case BodyBlock(id, html, _, title, _, _, _, publishedAt, _, updatedAt, _, _) if html.trim.nonEmpty =>
          TextBlock(id, title, publishedAt, updatedAt, html)
      }.take(number)
      Cached(page)(JsonComponent("blocks" -> Json.toJson(blocks)))
    case _ => Cached(600)(WithoutRevalidationResult(NotFound("Can only return block text for a live blog")))

  }

  private def noAMP(renderPage: => Result)(implicit  request: RequestHeader): Result = {
    if (request.isAmp) NotFound
    else renderPage
  }

  private def render(path: String, page: PageWithStoryPackage)(implicit request: RequestHeader) = page match {
    case blog: LiveBlogPage =>
      noAMP {
        val htmlResponse = () => views.html.liveBlog (blog)
        val jsonResponse = () => views.html.liveblog.liveBlogBody (blog)
        renderFormat(htmlResponse, jsonResponse, blog, Switches.all)
      }

    case minute: MinutePage =>
      noAMP {
        val htmlResponse = () => {
          if (request.isEmail) views.html.articleEmail(minute)
          else                 views.html.minute(minute)
        }

        val jsonResponse = () => views.html.fragments.minuteBody(minute)
        renderFormat(htmlResponse, jsonResponse, minute, Switches.all)
      }

    case article: ArticlePage =>
      val htmlResponse = () => {
        if (request.isEmail) views.html.articleEmail(article)
        else if (article.article.isImmersive) views.html.articleImmersive(article)
        else if (request.isAmp) views.html.articleAMP(article)
        else views.html.article(article)
      }

      val jsonResponse = () => views.html.fragments.articleBody(article)
      renderFormat(htmlResponse, jsonResponse, article, Switches.all)
  }

  def renderLiveBlog(path: String, page: Option[String] = None) =
    Action.async { implicit request =>
      mapModel(path, Some(page.map(a => PageWithBlock(ParseBlockId(a))).getOrElse(Canonical))) {// temporarily only ask for blocks too for things we know are new live blogs until until the migration is done and we can always use blocks
        render(path, _)
      }
    }

  def renderLiveBlogJson(path: String, lastUpdate: Option[String], rendered: Option[Boolean]) = {
    Action.async { implicit request =>
        (lastUpdate.flatMap(a => ParseBlockId(a)), rendered) match {
          case (Some(lastUpdate), _) => mapModel(path, range = Some(SinceBlockId(lastUpdate))) {
            case model: LiveBlogPage => renderNewerUpdates(model, lastUpdate)
          }
          case (None, Some(false)) => mapModel(path, range = Some(Canonical)) { model => blockText(model, 6) }
          case (_, _) => mapModel(path, range = Some(Canonical)) { model => render(path, model) }
        }
    }
  }

  def renderJson(path: String) = {
    Action.async { implicit request =>
      mapModel(path) {
        render(path, _)
      }
    }
  }

  def renderArticle(path: String) = {
    Action.async { implicit request =>
      mapModel(path, range = if (request.isEmail) Some(Canonical) else None) {
        render(path, _)
      }
    }
  }

  sealed trait Range
  case object Canonical extends Range
  case class PageWithBlock(page: Option[String]) extends Range
  case class SinceBlockId(lastUpdate: String) extends Range

  def mapModel(path: String, range: Option[Range] = None)(render: PageWithStoryPackage => Result)(implicit request: RequestHeader): Future[Result] = {
    val blocksString = range.map(_ match {
      case Canonical => "body:latest:29"// this only makes sense for liveblogs at the moment, but article use field body not blocks anyway
      case PageWithBlock(_) => "body" // just get them all, the caching should prevent too many requests, could use "around"
      case SinceBlockId(lastUpdate) => s"body:around:$lastUpdate:5" // more than 5 wouldn't come in (in one go), but never mind
    })
    lookup(path, blocksString) map responseToModelOrResult(range, blocksString) recover convertApiExceptions map {
      case Left(model) => render(model)
      case Right(other) => RenderOtherStatus(other)
    }
  }

  private def lookup(path: String, blocksString: Option[String])(implicit request: RequestHeader): Future[ItemResponse] = {
    val edition = Edition(request)


    log.info(s"Fetching article: $path for edition ${edition.id}: ${RequestLog(request)}")
    val capiItem = ContentApiClient.item(path, edition)
      .showTags("all")
      .showFields("all")
      .showReferences("all")
      .showAtoms("all")

    val capiItemWithBlocks = blocksString.map(capiItem.showBlocks(_)).getOrElse(capiItem)
    ContentApiClient.getResponse(capiItemWithBlocks)

  }

  /**
   * convert a response into something we can render, and return it
   * optionally, throw a response if we know it's not right to send the content
    *
    * @param response
   * @return Either[PageWithStoryPackage, Result]
   */
  def responseToModelOrResult(range: Option[Range], blocksString: Option[String])(response: ItemResponse)(implicit request: RequestHeader): Either[PageWithStoryPackage, Result] = {
    val supportedContent = response.content.filter(isSupported).map(Content(_))
    val supportedContentResult = ModelOrResult(supportedContent, response)
    val content: Either[PageWithStoryPackage, Result] = supportedContentResult.left.flatMap { content =>
      (content, range) match {
        case (minute: Article, Some(Canonical)) if minute.isUSMinute =>
          Left(MinutePage(minute, StoryPackages(minute, response)))
        case (liveBlog: Article, Some(Canonical)/*no page param*/) if liveBlog.isLiveBlog =>
          createLiveBlogModel(liveBlog, response, None)
        case (liveBlog: Article, Some(PageWithBlock(Some(requiredBlockId)))/*page param specified and valid format*/) if liveBlog.isLiveBlog =>
          createLiveBlogModel(liveBlog, response, Some(requiredBlockId))
        case (liveBlog: Article, Some(SinceBlockId(lastUpdate))/*page param specified and valid format*/) if liveBlog.isLiveBlog =>
          createLiveBlogModelSince(liveBlog, response, lastUpdate, blocksString.filter(_ == "body"))
        case (article: Article, None) => Left(ArticlePage(article, StoryPackages(article, response)))
        case _ =>
          Right(NotFound)
      }
    }

    content
  }

  def createLiveBlogModelSince(liveBlog: Article, response: ItemResponse, sinceBlockId: String, blocksString: Option[String]) = {

    val blocks = blocksString.map(liveBlog.content.fields.).getOrElse(liveBlog.content.fields.blocks)
    val liveBlogPageModel = LiveBlogCurrentPage(
      blocks,
      sinceBlockId
    )
    // only cache a "no blocks yet" response for a short time, but once something's there, let fastly cache it
    val cacheTime =
      if (liveBlog.fields.isLive && liveBlogPageModel.currentPage.blocks.isEmpty)
        liveBlog.metadata.cacheTime
      else
        CacheTime.NotRecentlyUpdated

    val liveBlogCache = liveBlog.copy(
      content = liveBlog.content.copy(
        metadata = liveBlog.content.metadata.copy(
          cacheTime = cacheTime)))
    Left(LiveBlogPage(liveBlogCache, liveBlogPageModel, StoryPackages(liveBlog, response)))

  }

  def createLiveBlogModel(liveBlog: Article, response: ItemResponse, maybeRequiredBlockId: Option[String]) = {

    val pageSize = if (liveBlog.content.tags.tags.map(_.id).contains("sport/sport")) 30 else 10
    val liveBlogPageModel = LiveBlogCurrentPage(
      pageSize = pageSize,
      liveBlog.content.fields.blocks,
      maybeRequiredBlockId
    )
    liveBlogPageModel match {
      case Some(pageModel) =>

        val cacheTime =
          if (!pageModel.currentPage.isArchivePage && liveBlog.fields.isLive)
            liveBlog.metadata.cacheTime
          else if (liveBlog.fields.lastModified > DateTime.now(liveBlog.fields.lastModified.getZone) - 1.hour)
            CacheTime.RecentlyUpdated
          else if (liveBlog.fields.lastModified > DateTime.now(liveBlog.fields.lastModified.getZone) - 24.hours)
            CacheTime.LastDayUpdated
          else
            CacheTime.NotRecentlyUpdated

        val liveBlogCache = liveBlog.copy(
          content = liveBlog.content.copy(
            metadata = liveBlog.content.metadata.copy(
              cacheTime = cacheTime)))
        Left(LiveBlogPage(liveBlogCache, pageModel, StoryPackages(liveBlog, response)))
      case None => Right(NotFound)
    }

  }

}

object ParseBlockId extends RegexParsers {
  def apply(input: String): Option[String] = {
    def withParser: Parser[Unit] = "with:" ^^ { _ => () }
    def block: Parser[Unit] = "block-" ^^ { _ => () }
    def id: Parser[String] = "[a-zA-Z0-9]+".r
    def expr: Parser[String] = withParser ~> block ~> id

    parse(expr, input) match {
      case Success(matched, _) => Some(matched)
      case _ => None
    }
  }
}

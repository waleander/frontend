package model

import conf.switches.Switches._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.http.Writeable
import play.api.mvc._
import scala.math.{min, max}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class CacheTime(cacheSeconds: Int)
object CacheTime {

  // 3800 seems slightly arbitrary, but our CDN caches to disk if above 3700
  // https://community.fastly.com/t/why-isnt-serve-stale-working-as-expected/369
  private def extended(cacheTime: Int) = if (LongCacheSwitch.isSwitchedOn) 3800 else cacheTime

  object Default extends CacheTime(60)
  object LiveBlogActive extends CacheTime(5)
  object RecentlyUpdated extends CacheTime(60)
  object Facia extends CacheTime(300)
  object NotFound extends CacheTime(10) // This will be overwritten by fastly

  def LastDayUpdated = CacheTime(extended(60))
  def NotRecentlyUpdated = CacheTime(extended(300))
  def NotRecentlyUpdatedPurgable = CacheTime(extended(1800))
}

object Cached extends implicits.Dates {

  private val cacheableStatusCodes = Seq(200, 404)

  private val tenDaysInSeconds = 864000

  case class Hash(string: String)

  sealed trait CacheableResult { def result: Result }
  case class RevalidatableResult(result: Result, hash: Hash) extends CacheableResult
  case class WithoutRevalidationResult(result: Result) extends CacheableResult

  object RevalidatableResult {
    def apply[C](result: Result, content: C)(implicit writeable: Writeable[C]) = {
      // hashing function from Arrays.java
      val hashLong: Long = writeable.transform(content).foldLeft(z = 1L){
        case (accu, nextByte) => 31 * accu + nextByte
      }
      new RevalidatableResult(result, Hash(hashLong.toString))
    }

    def Ok[C](content: C)(implicit writeable: Writeable[C]) = {
      apply(Results.Ok(content), content)
    }
  }


  def apply(seconds: Int)(result: CacheableResult)(implicit request: RequestHeader): Result = {
    apply(seconds, result, request.headers.get("If-None-Match"))//FIXME could be comma separated
  }

  def apply(cacheTime: CacheTime)(result: CacheableResult)(implicit request: RequestHeader): Result = {
    apply(cacheTime.cacheSeconds, result, request.headers.get("If-None-Match"))
  }

  def apply(duration: Duration)(result: CacheableResult)(implicit request: RequestHeader): Result = {
    apply(duration.toSeconds.toInt, result, request.headers.get("If-None-Match"))
  }

  def apply(page: Page)(revalidatableResult: CacheableResult)(implicit request: RequestHeader): Result = {
    val cacheSeconds = page.metadata.cacheTime.cacheSeconds
    apply(cacheSeconds, revalidatableResult, request.headers.get("If-None-Match"))
  }

  // Use this when you are sure your result needs caching headers, even though the result status isn't
  // conventionally cacheable. Typically we only cache 200 and 404 responses.
  def explicitlyCache(seconds: Int)(result: Result): Result = cacheHeaders(seconds, result, None)

  def apply(seconds: Int, cacheableResult: CacheableResult, ifNoneMatch: Option[String]) =
    if (cacheableStatusCodes.contains(cacheableResult.result.header.status)) {
      cacheableResult match {
        case RevalidatableResult(result, hash) =>
          cacheHeaders(seconds, result, Some((hash, ifNoneMatch)))
        case WithoutRevalidationResult(result) => cacheHeaders(seconds, result, None)
      }
    } else {
      cacheableResult.result
    }

  /*
    NOTE, if you change these headers make sure they are compatible with our Edge Cache

    see
    http://tools.ietf.org/html/rfc5861
    http://www.fastly.com/blog/stale-while-revalidate
    http://docs.fastly.com/guides/22966608/40347813

    This explains Surrogate-Control vs Cache-Control
    TLDR Surrogate-Control is used by the CDN, Cache-Control by the browser - do *not* add `private` to Cache-Control
    https://docs.fastly.com/guides/tutorials/cache-control-tutorial
  */
  private def cacheHeaders(maxAge: Int, result: Result, maybeHash: Option[(Hash, Option[String])]) = {
    val now = DateTime.now
    val expiresTime = if (LongCacheSwitch.isSwitchedOn) now + min(maxAge, 60).seconds else now + maxAge.seconds

    val staleWhileRevalidateSeconds = max(maxAge / 10, 1)
    val surrogateCacheControl = s"max-age=$maxAge, stale-while-revalidate=$staleWhileRevalidateSeconds, stale-if-error=$tenDaysInSeconds"

    val cacheControl = if (LongCacheSwitch.isSwitchedOn) {
      val browserMaxAge = min(maxAge, 60)
      val browserStaleWhileRevalidateSeconds = max(browserMaxAge / 10, 1)
      s"max-age=$browserMaxAge, stale-while-revalidate=$browserStaleWhileRevalidateSeconds, stale-if-error=$tenDaysInSeconds"
    } else {
      surrogateCacheControl
    }

    val (etagHeaderString, validatedResult): (String, Result) = maybeHash.map { case (hash, maybeHashToMatch) =>
      val etag = s"""W/"hash${hash.string}""""
      if (maybeHashToMatch.contains(etag)) {
        (etag, Results.NotModified)
      } else {
        (etag, result)
      }
    }.getOrElse(
      (s""""johnRandom${scala.util.Random.nextInt}${scala.util.Random.nextInt}"""", result) // just to see if they come back in
    )

    validatedResult.withHeaders(

      // the cache headers used by the CDN
      "Surrogate-Control" -> surrogateCacheControl,
      // the cache headers that make their way through to the browser
      "Cache-Control" -> cacheControl,

      "Expires" -> expiresTime.toHttpDateTimeString,
      "Date" -> now.toHttpDateTimeString,
      "ETag" -> etagHeaderString)
  }
}

object NoCache {
  def apply(result: Result): Result = result.withHeaders("Cache-Control" -> "no-cache", "Pragma" -> "no-cache")
}

case class NoCache[A](action: Action[A]) extends Action[A] {

  override def apply(request: Request[A]): Future[Result] = {

    action(request) map { response =>
      response.withHeaders(
        ("Cache-Control", "no-cache, no-store, must-revalidate"),
        ("Pragma", "no-cache"),
        ("Expires", "0")
      )
    }
  }

  lazy val parser = action.parser
}

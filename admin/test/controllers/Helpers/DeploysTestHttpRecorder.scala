package controllers.Helpers

import java.io.{File, InputStream}
import java.nio.ByteBuffer
import java.util
import com.ning.http.client.uri.Uri
import com.ning.http.client.{FluentCaseInsensitiveStringsMap, Response => ningResponse}
import play.api.libs.ws.WSResponse
import recorder.{WsHttpRecorder, HttpRecorder}

object DeploysTestHttpRecorder extends HttpRecorder[WSResponse] with WsHttpRecorder[WSResponse] {
  override lazy val baseDir = new File(System.getProperty("user.dir"), "data/deploys")

  val errorPrefix = "Error:"
  override def toResponse(str: String) = {
    if (str.startsWith(errorPrefix)) {
      val status = str.replace(errorPrefix, "").toInt
      toResponse(status, str)
    } else {
      toResponse(200, str)
    }
  }
}

private case class Response(getResponseBody: String, status: Int) extends ningResponse {
  def getContentType: String = "application/json"
  def getResponseBody(charset: String): String = getResponseBody
  def getStatusCode: Int = status
  def getResponseBodyAsBytes: Array[Byte] = getResponseBody.getBytes
  def getResponseBodyAsByteBuffer: ByteBuffer = throw new NotImplementedError()
  def getResponseBodyAsStream: InputStream = throw new NotImplementedError()
  def getResponseBodyExcerpt(maxLength: Int, charset: String): String = throw new NotImplementedError()
  def getResponseBodyExcerpt(maxLength: Int): String = throw new NotImplementedError()
  def getStatusText: String = throw new NotImplementedError()
  def getUri: Uri = throw new NotImplementedError()
  def getHeader(name: String): String = throw new NotImplementedError()
  def getHeaders(name: String): util.List[String] = throw new NotImplementedError()
  def getHeaders: FluentCaseInsensitiveStringsMap = throw new NotImplementedError()
  def isRedirected: Boolean = throw new NotImplementedError()
  def getCookies = throw new NotImplementedError()
  def hasResponseStatus: Boolean = throw new NotImplementedError()
  def hasResponseHeaders: Boolean = throw new NotImplementedError()
  def hasResponseBody: Boolean = throw new NotImplementedError()
}


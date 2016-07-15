package jobs

import java.net.URI

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import common._
import conf.Configuration
import conf.switches.Switches.TeacherResourcePressSwitch
import implicits.TeacherResourcePressNotification.pressMessageFormatter
import model.TeacherResourcePressMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import pagepresser._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import services.{S3TeacherResource, S3TeacherResourceOriginals}

import scala.concurrent.Future

object TeacherResourcePagePressJob extends ExecutionContexts with Logging {
  private val waitTimeSeconds = Configuration.teacherResource.pressQueueWaitTimeInSeconds
  private val maxMessages = Configuration.teacherResource.pressQueueMaxMessages
  private val credentials = Configuration.aws.mandatoryCredentials

  def run() = {
    if (TeacherResourcePressSwitch.isSwitchedOn) {
      log.info("TeacherResourcePagePressJob starting")
      try {
        val pressing = queue.receive(new ReceiveMessageRequest()
          .withWaitTimeSeconds(waitTimeSeconds)
          .withMaxNumberOfMessages(maxMessages)
        ).flatMap( messages => Future.sequence(messages map press).map(_ => ()) )

        val takingDown = takedownQueue.receive(new ReceiveMessageRequest()
          .withWaitTimeSeconds(waitTimeSeconds)
          .withMaxNumberOfMessages(maxMessages)
        ).map ( messages => Future.sequence(messages map takedown).map(_ => ()) )

        Future.sequence(Seq(pressing, takingDown)).map(_ => ())
      } catch {
        case e: Exception => log.error(s"Failed to decode url: ${e.getMessage}", e)
          Future.failed(new RuntimeException(s"Failed to decode url: ${e.getMessage}", e))
      }
    } else {
      log.info("TeacherResourcePagePressJob is switched OFF")
      Future.successful(())
    }
  }

  private val queue: JsonMessageQueue[SNSNotification] = (Configuration.teacherResource.sqsQueueUrl map { queueUrl =>
    JsonMessageQueue[SNSNotification](
      new AmazonSQSAsyncClient(credentials).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      queueUrl
    )
  }) getOrElse {
    throw new RuntimeException("Required property 'teacherResource.sqsQueueUrl' not set")
  }

  private val takedownQueue: TextMessageQueue[SNSNotification] = (Configuration.teacherResource.sqsTakedownQueueUrl map { queueUrl =>
    TextMessageQueue[SNSNotification](
      new AmazonSQSAsyncClient(credentials).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      queueUrl
    )
  }) getOrElse {
    throw new RuntimeException("Required property 'teacherResource.sqsTakedownQueueUrl' not set")
  }

  private def extractMessage(notification: Message[SNSNotification]): TeacherResourcePressMessage = {
    Json.parse(notification.get.Message).as[TeacherResourcePressMessage]
  }

  private def press(notification: Message[SNSNotification]): Future[Unit] = {
    val pressMessage = extractMessage(notification)
    if (pressMessage.fromPreservedSrc){
      pressFromOriginalSource(notification)
    } else {
      pressFromLive(notification)
    }
  }

  private def pressAsUrl(urlIn: String): String = {
    val uri = URI.create(urlIn)
    val pressPath = (uri.getPath + (if(uri.getQuery == null) "" else "?" + uri.getQuery)).tail
    if (pressPath.endsWith("/")) pressPath.dropRight(1) else pressPath
  }

  private def parseAndClean(originalDocSource: String, convertToHttps: Boolean): Future[String] = {
    val cleaners = Seq(TeacherResourceHtmlCleaner)
    val archiveDocument = Jsoup.parse(originalDocSource)
    val testDoc = cleaners.filter(_.canClean(archiveDocument))
      .map(_.clean(archiveDocument, convertToHttps))
      .headOption

    val doc = if (testDoc.isDefined) {
      log.info("Success")
      testDoc.get
    } else {
      log.error("No suitable cleaners exist for document. Original source used.")
      archiveDocument
    }

    Future.successful(doc.toString)

  }

  private def S3TeacherResourcePutAndCheck(pressUrl: String, cleanedHtml: String) = {
    S3TeacherResource.putPublic(pressUrl, cleanedHtml, "text/html")
    S3TeacherResource.get(pressUrl).exists { result =>
      if (result == cleanedHtml) {
        true
      } else {
        log.error(s"Pressed HTML did not match cleaned HTML for $pressUrl")
        false
      }
    }
  }

  private def pressFromOriginalSource(notification: Message[SNSNotification]): Future[Unit] = {
    val message = extractMessage(notification)
    val urlIn = message.url
    val pressUrl = pressAsUrl(urlIn)

    S3TeacherResourceOriginals.get(pressUrl).map { originalSource =>
      log.info(s"Re-pressing $urlIn from $pressUrl")

      val cleanedHtmlString = parseAndClean(originalSource, message.convertToHttps)

      cleanedHtmlString.map { cleanedHtmlString =>
        S3TeacherResourcePutAndCheck(pressUrl, cleanedHtmlString) match {
          case true => {
            log.info(s"Pressed $urlIn as $pressUrl")
            queue.delete(notification.handle)
          }
          case _ => {
            log.error(s"Press failed for $pressUrl")
          }
        }
      }.map(_ => ())
    }.getOrElse(Future.successful(()))

  }

  private def pressFromLive(notification: Message[SNSNotification]): Future[Unit] = {
    val message = extractMessage(notification)
    val urlIn = message.url

    if (urlIn.nonEmpty) {

      val wsRequest = WS.url(urlIn)

      log.info(s"Calling ${wsRequest.uri}")

      wsRequest.get().flatMap { response =>
        response.status match {
          case 200 => {
            try {
              val originalSource = response.body
              val pressUrl = pressAsUrl(urlIn)

              if (S3TeacherResourceOriginals.get(pressUrl).isEmpty) {
                S3TeacherResourceOriginals.putPublic(pressUrl, originalSource, "text/html")
                log.info(s"Original page source saved for $pressUrl")
              }

              val cleanedHtmlString = parseAndClean(originalSource, message.convertToHttps)

              cleanedHtmlString.map { cleanedHtmlString =>
                S3TeacherResourcePutAndCheck(pressUrl, cleanedHtmlString) match {
                  case true => {
                    log.info(s"Pressed $urlIn as $pressUrl")
                    queue.delete(notification.handle)
                  }
                  case _ => {
                    log.error(s"Press failed for $pressUrl")
                  }
                }
              }

            } catch {
              case e: Exception => log.error(s"Unable to press $urlIn (${e.getMessage})", e)
                Future.failed(new RuntimeException(s"Unable to press $urlIn (${e.getMessage})", e))
            }
          }
          case non200 => {
            log.error(s"Unexpected response from ${wsRequest.uri}, status code: $non200")
            Future.failed(new RuntimeException(s"Unexpected response from ${wsRequest.uri}, status code: $non200"))
          }
        }
      }
    } else {
      log.error(s"Invalid url: $urlIn")
      Future.failed(new RuntimeException(s"Invalid url: $urlIn"))
    }
  }

  private def takedown(message: Message[String]): Future[Unit] = {
    val urlIn = pressAsUrl((Json.parse(message.get) \ "Message").as[String])
    try {
      if (urlIn.nonEmpty) {
        //TODO!
        //Rename the html so it 404s from the browser?
        takedownQueue.delete(message.handle)
      } else {
        log.error(s"Invalid url: $urlIn")
        Future.failed(new RuntimeException(s"Invalid url: $urlIn"))
      }
    } catch {
      case e: Exception => log.error(s"Cannot take down $urlIn: ${e.getMessage}")
        Future.failed(new RuntimeException(s"Cannot take down $urlIn", e))
    }
  }

}

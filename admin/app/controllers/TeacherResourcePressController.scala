package controllers.admin

import java.io.File

import common.{ExecutionContexts, Logging}
import controllers.AuthLogging
import model.TeacherResourcePressMessage
import play.api.mvc.{AnyContent, Controller}
import services.{TeacherResourcePressNotifier, TeacherResourceTakedownNotifier}

class TeacherResourcePressController extends Controller with Logging with AuthLogging with ExecutionContexts {

  def pressForm(urlMsgs: List[String] = List.empty, fileMsgs: List[String] = List.empty) = AuthActions.AuthActionTest { implicit request =>
    Ok(views.html.pressTeacherResource(urlMsgs, fileMsgs))
  }

  def batchUpload() = AuthActions.AuthActionTest { implicit request =>
    val body = request.body
    val uploadedFile = body.asMultipartFormData.flatMap { files =>
      files.file("resourceUrlfile").map { theFile =>
        val rnd = Math.random().toString.replace(".","")
        val tmpName = s"/tmp/$rnd${theFile.filename}"
        val tmpFile = new File(tmpName)
        theFile.ref.moveTo(tmpFile)
        tmpFile
      }
    }
    val msgs = if(uploadedFile.nonEmpty) {
      val results = uploadedFile.map(file => {
        try {
          pressFile(file, isBatchTakedown(body), isBatchFromPreservedSource(body), isBatchConvertToHttps(body))
        } catch {
          case e: Exception => List(s"Error processing ${file.getName} - ${e.getMessage}")
        }
      })
      List(s"File uploaded as ${uploadedFile.map(_.getName).getOrElse("")}") ::: results.getOrElse(List.empty)
    } else {
      List("File was not uploaded")
    }

    Ok(views.html.pressTeacherResource(fileMsgs = msgs))
  }

  private def pressFile(file: File, isTakedown: Boolean, isFromPreservedSource: Boolean, isConvertToHttps: Boolean): List[String] = {
    val source = scala.io.Source.fromFile(file)
    try {
      source.getLines().map { line =>
        if (line.nonEmpty) {
          if (isTakedown) {
            TeacherResourceTakedownNotifier.enqueue(line)
          } else {
            TeacherResourcePressNotifier.enqueue(TeacherResourcePressMessage(line, isFromPreservedSource, isConvertToHttps))
          }
        } else {
          "* empty line *"
        }
      }.toList
    } finally {
      source.close()
      file.delete()
    }
  }

  def press() = AuthActions.AuthActionTest { implicit request =>
    val body = request.body
    val result = body.asFormUrlEncoded.map { form =>
      form("resourceUrl").map { resourceUrl =>
        resourceUrl.trim match {
          case url if url.nonEmpty => {
            if (isTakedown(body)) {
              TeacherResourceTakedownNotifier.enqueue(url)
            } else {
              TeacherResourcePressNotifier.enqueue(TeacherResourcePressMessage(url, isFromPreservedSource(body), isConvertToHttps(body)))
            }
          }
          case _ => "URL was not specified"
        }
      }
    }.map(_.toList).getOrElse(List.empty)
    Ok(views.html.pressTeacherResource(urlMsgs = result))
  }

  private def isTakedown(body: AnyContent) = {
    body.asFormUrlEncoded.flatMap { form =>
      Some(form.get("is-takedown").isDefined)
    }.getOrElse(false)
  }

  private def isFromPreservedSource(body: AnyContent) = {
    body.asFormUrlEncoded.flatMap { form =>
      Some(form.get("is-from-preserved-source").isDefined)
    }.getOrElse(false)
  }

  private def isConvertToHttps(body: AnyContent) = {
    body.asFormUrlEncoded.flatMap { form =>
      Some(form.get("is-convert-to-https").isDefined)
    }.getOrElse(false)
  }

  private def isBatchTakedown(body: AnyContent) = {
    body.asMultipartFormData.flatMap { form =>
      Some(form.asFormUrlEncoded.get("is-takedown").isDefined)
    }.getOrElse(false)
  }

  private def isBatchFromPreservedSource(body: AnyContent) = {
    body.asMultipartFormData.flatMap { form =>
      Some(form.asFormUrlEncoded.get("is-from-preserved-source").isDefined)
    }.getOrElse(false)
  }

  private def isBatchConvertToHttps(body: AnyContent) = {
    body.asMultipartFormData.flatMap { form =>
      Some(form.asFormUrlEncoded.get("is-convert-to-https").isDefined)
    }.getOrElse(false)
  }

}

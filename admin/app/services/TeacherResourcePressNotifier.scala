package services

import common.Logging
import implicits.TeacherResourcePressNotification.pressMessageFormatter
import model.TeacherResourcePressMessage
import play.api.libs.json.Json

object TeacherResourcePressNotifier extends Logging {

  def enqueue(message: TeacherResourcePressMessage): String = {
    try {
      TeacherResourcePressNotification.sendWithoutSubject(Json.toJson[TeacherResourcePressMessage](message).toString())
      val msg = s"Queued for pressing: ${message.url} (from preserved source: ${message.fromPreservedSrc})"
      log.info(msg)
      msg
    } catch {
      case e: Exception => {
        val msg = s"Failed to add ${message.url} to the r2 page press queue"
        log.error(msg, e)
        msg
      }
    }
  }

}

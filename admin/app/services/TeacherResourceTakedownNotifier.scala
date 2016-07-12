package services

import common.Logging

object TeacherResourceTakedownNotifier extends Logging {

  def enqueue(path: String): String = {
    try {
      TeacherResourceTakedownNotification.sendWithoutSubject(path)
      val msg = s"Queued for takedown: $path"
      log.info(msg)
      msg
    } catch {
      case e: Exception => {
        val msg = s"Failed to add $path to the teacher resource takedown queue"
        log.error(msg, e)
        msg
      }
    }
  }

}

package implicits

import play.api.libs.functional.syntax._
import play.api.libs.json._
import model.TeacherResourcePressMessage

trait TeacherResourcePressNotification {
  implicit val pressMessageFormatter: Format[TeacherResourcePressMessage] = (
    (__ \ "url").format[String] and
      (__ \ "fromPreservedSrc").format[Boolean] and
        (__ \ "convertToHttps").format[Boolean]
    )(TeacherResourcePressMessage.apply, unlift(TeacherResourcePressMessage.unapply))
}

object TeacherResourcePressNotification extends TeacherResourcePressNotification

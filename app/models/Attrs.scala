package models

import play.api.libs.typedmap.TypedKey

object Attrs {
  val RequestId: TypedKey[String] = TypedKey[String]("RequestId")
  val SessionId: TypedKey[String] = TypedKey[String]("SessionId")
}

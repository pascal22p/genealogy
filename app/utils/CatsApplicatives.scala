package utils

import cats.Applicative
import cats.ApplicativeError
import play.api.libs.json.*

object CatsApplicatives {
  // https://github.com/iravid/play-json-cats/blob/732f50cf79af5a4e329d7348c753a34c4326cb4d/src/main/scala/com/iravid/playjsoncats/JsResultInstances.scala#L14
  implicit val jsResultApplicativeAndApplicativeError: Applicative[JsResult] & ApplicativeError[JsResult, JsError] =
    new Applicative[JsResult] with ApplicativeError[JsResult, JsError] {
      def pure[A](x: A) = JsSuccess(x)

      override def map[A, B](fa: JsResult[A])(f: A => B): JsResult[B] = fa.map(f)

      def ap[A, B](ff: JsResult[A => B])(fa: JsResult[A]): JsResult[B] =
        (ff, fa) match {
          case (JsSuccess(f, _), JsSuccess(a, _)) => JsSuccess(f(a))
          case (fe: JsError, ae: JsError)         => fe ++ ae
          case (fe: JsError, _)                   => fe
          case (_, ae: JsError)                   => ae
        }

      def raiseError[A](e: JsError): JsResult[A] = e

      def handleErrorWith[A](fa: JsResult[A])(f: JsError => JsResult[A]): JsResult[A] =
        fa match {
          case s @ JsSuccess(_, _) => s
          case e: JsError          => f(e)
        }
    }
}

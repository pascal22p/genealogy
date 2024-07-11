package models

import play.api.mvc.Request
import play.api.mvc.WrappedRequest

case class AuthenticatedRequest[A](request: Request[A], dbId: Int) extends WrappedRequest[A](request)

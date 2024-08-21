package models

import play.api.mvc.Request
import play.api.mvc.WrappedRequest

final case class AuthenticatedRequest[A](request: Request[A], localSession: Session) extends WrappedRequest[A](request)

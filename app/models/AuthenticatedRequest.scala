package models

import play.api.mvc.Request
import play.api.mvc.WrappedRequest

final case class AuthenticatedRequest[A](
    request: Request[A],
    localSession: Session,
    genealogyDatabase: Option[GenealogyDatabase]
) extends WrappedRequest[A](request) {
  def isAdmin: Boolean = localSession.sessionData.userData.exists(_.isAdmin)
}

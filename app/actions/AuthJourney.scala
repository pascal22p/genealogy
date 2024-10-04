package actions

import com.google.inject.ImplementedBy
import play.api.mvc.ActionBuilder
import models.AuthenticatedRequest
import play.api.mvc.AnyContent
import javax.inject.Inject


@ImplementedBy(classOf[AuthJourneyImpl])
trait AuthJourney {
  val authWithAdminRight: ActionBuilder[AuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
  authAction: AuthAction,
  adminFilter: AdminFilter
) extends AuthJourney {

  val authWithAdminRight: ActionBuilder[AuthenticatedRequest, AnyContent] =
    authAction andThen adminFilter
}
package actions

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.AuthenticatedRequest
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent

@ImplementedBy(classOf[AuthJourneyImpl])
trait AuthJourney {
  val authWithAdminRight: ActionBuilder[AuthenticatedRequest, AnyContent]
}

class AuthJourneyImpl @Inject() (
    authAction: AuthAction,
    adminFilter: AdminFilter
) extends AuthJourney {

  val authWithAdminRight: ActionBuilder[AuthenticatedRequest, AnyContent] =
    authAction.andThen(adminFilter)
}

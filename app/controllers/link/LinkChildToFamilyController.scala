package controllers.link

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import anorm.NamedParameter
import cats.implicits.*
import models.forms.LinkForm
import models.Attributes
import models.AuthenticatedRequest
import models.EventType.IndividualEvent
import models.Events
import models.MediaType.UnknownMedia
import models.Person
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.GetSqlQueries
import queries.InsertSqlQueries
import services.EventService
import services.FamilyService
import views.html.link.LinkChildToFamilyView

@Singleton
class LinkChildToFamilyController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    getSqlQueries: GetSqlQueries,
    familyService: FamilyService,
    eventService: EventService,
    linkChildToFamilyView: LinkChildToFamilyView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(dbId: Int, familyId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = LinkForm.linkForm
      for {
        allPersonsDetails <- getSqlQueries.getAllPersonDetails(dbId)
        allPersons        <- allPersonsDetails.traverse { person =>
          eventService.getIndividualEvents(person.id).map { events =>
            Person(
              person,
              Events(events, Some(person.id), IndividualEvent),
              Attributes(List.empty, Some(person.id), IndividualEvent),
              List.empty,
              List.empty
            )
          }
        }
        family <- familyService.getFamilyDetails(familyId, true).value
      } yield {
        family.fold(NotFound(s"Family id $familyId not found")) { someFamily =>
          Ok(linkChildToFamilyView(dbId, allPersons, someFamily, form))
        }
      }
  }

  def onSubmit(dbId: Int, familyId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      // TODO: finish this
      val errorFunction: Form[LinkForm] => Future[Result] = { (_: Form[LinkForm]) =>
        getSqlQueries.getMedias(None, UnknownMedia, dbId).map { _ =>
          BadRequest("linkSourCitationToMediaView(dbId, sourCitationId, formWithErrors, allMedias)")
        }
      }

      val successFunction: LinkForm => Future[Result] = { (dataForm: LinkForm) =>
        insertSqlQueries
          .linkTable(
            "rel_familles_indi",
            List(
              NamedParameter("familles_id", familyId),
              NamedParameter("indi_id", dataForm.linkId),
              NamedParameter("rela_type", "BIRTH"),
              NamedParameter("rela_stat", Option.empty[String])
            )
          )
          .map { _ =>
            Redirect(controllers.routes.FamilyController.showFamily(dbId, familyId))
          }
      }

      val formValidationResult = LinkForm.linkForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}

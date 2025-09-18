package controllers.add

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import cats.implicits.*
import config.AppConfig
import models.forms.FamilyForm
import models.forms.LinkForm
import models.forms.TrueOrFalseForm
import models.Attributes
import models.AuthenticatedRequest
import models.EventType.IndividualEvent
import models.Events
import models.Person
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import queries.GetSqlQueries
import queries.InsertSqlQueries
import queries.UpdateSqlQueries
import services.EventService
import services.FamilyService
import services.PersonService
import views.html.add.AddExistingFamilyView
import views.html.add.AddFamilyView
import views.html.add.ShowAddPersonToFamiliyInterstitialView

class AddPersonToFamily @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    eventService: EventService,
    familyService: FamilyService,
    getSqlQueries: GetSqlQueries,
    insertSqlQueries: InsertSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    showInterstitialView: ShowAddPersonToFamiliyInterstitialView,
    addFamilyView: AddFamilyView,
    addExistingFamilyView: AddExistingFamilyView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends BaseController
    with I18nSupport {

  def showInterstitial(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(personId, true, true, true).map { maybePerson =>
        maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
          val form = TrueOrFalseForm.trueOrFalseForm.fill(TrueOrFalseForm(true))
          Ok(showInterstitialView(baseId, person, form))
        }
      }
  }

  def submitInterstitial(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[TrueOrFalseForm] => Future[Result] = { (formWithErrors: Form[TrueOrFalseForm]) =>
        personService.getPerson(personId, true, true, true).map { maybePerson =>
          maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
            BadRequest(showInterstitialView(baseId, person, formWithErrors))
          }
        }
      }

      val successFunction: TrueOrFalseForm => Future[Result] = { (dataForm: TrueOrFalseForm) =>
        if (dataForm.trueOrFalse) {
          Future.successful(Redirect(controllers.add.routes.AddPersonToFamily.showNewFamilyForm(baseId, personId)))
        } else {
          Future.successful(Redirect(controllers.add.routes.AddPersonToFamily.showExistingFamilyForm(baseId, personId)))
        }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def showNewFamilyForm(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        allPersonsDetails <- getSqlQueries.getAllPersonDetails(baseId)
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
        maybePerson <- personService.getPerson(personId, true, true, true)
      } yield {
        maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
          val form = FamilyForm.familyForm.fill(FamilyForm(baseId, Some(personId), None, None))
          Ok(addFamilyView(baseId, allPersons, person, form))
        }
      }
  }

  def submitNewFamilyForm(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[FamilyForm] => Future[Result] = { (formWithErrors: Form[FamilyForm]) =>
        for {
          allPersonsDetails <- getSqlQueries.getAllPersonDetails(baseId)
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
          maybePerson <- personService.getPerson(personId, true, true, true)
        } yield {
          maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
            BadRequest(addFamilyView(baseId, allPersons, person, formWithErrors))
          }
        }
      }

      val successFunction: FamilyForm => Future[Result] = { (dataForm: FamilyForm) =>
        insertSqlQueries.insertFamily(dataForm.familyQueryData).fold(InternalServerError("No family was inserted")) {
          familyId =>
            Redirect(controllers.routes.FamilyController.showFamily(baseId, familyId))
        }
      }

      val formValidationResult = FamilyForm.familyForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def showExistingFamilyForm(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        allFamilies <- familyService.getAllFamilies(baseId)
        maybePerson <- personService.getPerson(personId, true, true, true)
      } yield {
        maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
          val form = LinkForm.linkForm
          Ok(
            addExistingFamilyView(
              baseId,
              allFamilies.filter(family =>
                (family.parent1.isEmpty || family.parent2.isEmpty) &&
                  !family.parent1.map(_.details.id).contains(personId) &&
                  !family.parent2.map(_.details.id).contains(personId)
              ),
              person,
              form
            )
          )
        }
      }
  }

  def submitExistingFamilyForm(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[LinkForm] => Future[Result] = { (formWithErrors: Form[LinkForm]) =>
        for {
          allFamilies <- familyService.getAllFamilies(baseId)
          maybePerson <- personService.getPerson(personId, true, true, true)
        } yield {
          maybePerson.fold(NotFound(s"Person with id $personId cannot be found")) { person =>
            BadRequest(
              addExistingFamilyView(
                baseId,
                allFamilies.filter(family =>
                  (family.parent1.isEmpty || family.parent2.isEmpty) &&
                    !family.parent1.map(_.details.id).contains(personId) &&
                    !family.parent2.map(_.details.id).contains(personId)
                ),
                person,
                formWithErrors
              )
            )
          }
        }
      }

      val successFunction: LinkForm => Future[Result] = { (dataForm: LinkForm) =>
        updateSqlQueries
          .updatePartnerFromFamily(personId, dataForm.linkId)
          .map { _ =>
            Redirect(controllers.routes.FamilyController.showFamily(baseId, dataForm.linkId))
          }
      }

      val formValidationResult = LinkForm.linkForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}

package controllers.add

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import cats.implicits.toTraverseOps
import config.AppConfig
import models.forms.IntegerForm
import models.forms.StringForm
import models.journeyCache.UserAnswersKey.SearchIndividualForNewFamilyQuestion
import models.journeyCache.UserAnswersKey.SelectIndividualFromSearch
import models.journeyCache.UserAnswersKey.SelectLatestIndividualForNewFamilyQuestion
import models.journeyCache.UserAnswersKey.SelectedDatabaseHidden
import models.journeyCache.UserAnswersKey.SelectedFamilyIdHidden
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import repositories.JourneyCacheRepository
import services.FamilyService
import services.GenealogyDatabaseService
import services.PersonService
import views.html.add.addPersonAsPartner.LatestIndividualsSelectionView
import views.html.add.addPersonAsPartner.SearchIndividualView
import views.html.add.addPersonAsPartner.SearchResultsView
import models.forms.extensions.FillFormExtension.filledWith
import queries.UpdateSqlQueries
import views.html.add.addPersonAsPartner.CheckYourAnswersView

@Singleton
class AddPersonAsPartnerController @Inject() (
    journeyCacheRepository: JourneyCacheRepository,
    genealogyDatabaseService: GenealogyDatabaseService,
    updateSqlQueries: UpdateSqlQueries,
    personService: PersonService,
    familyService: FamilyService,
    appConfig: AppConfig,
    authJourney: AuthJourney,
    lastIndividualView: LatestIndividualsSelectionView,
    searchIndividualView: SearchIndividualView,
    searchResultsView: SearchResultsView,
    checkYourAnswersView: CheckYourAnswersView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def startAddIndividualToFamily(dbId: Int, familyId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        db <- genealogyDatabaseService.getGenealogyDatabases.map(
          _.find(_.id == dbId).getOrElse(throw new Exception("Database not found"))
        )
        _      <- journeyCacheRepository.upsert(SelectedDatabaseHidden, IntegerForm(db.id, s"${db.name} (${db.id})"))
        family <- familyService
          .getFamilyDetails(familyId)
          .value
          .map(_.getOrElse(throw new Exception("Family not found")))
          .map(family =>
            if (family.parent1.isDefined && family.parent2.isDefined) {
              throw new RuntimeException(s"Family $familyId already has two partners")
            } else { family }
          )
        _ <- journeyCacheRepository.upsert(
          SelectedFamilyIdHidden,
          IntegerForm(familyId, s"${family.parent1.fold("")(_.shortName)} - ${family.parent2.fold("")(_.shortName)}")
        )
      } yield {
        Redirect(controllers.add.routes.AddPersonAsPartnerController.selectLatestIndividual)
      }
  }

  def selectLatestIndividual: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(SelectLatestIndividualForNewFamilyQuestion).flatMap { defaults =>
        val form = IntegerForm.integerFormWithLabel.filledWith(defaults)
        personService.getLatestPersons(1, 10).map { indis =>
          Ok(lastIndividualView(1, indis, form)(using implicitly, implicitly, appConfig))
        }
      }
  }

  def selectLatestIndividualOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      IntegerForm.integerFormWithLabel
        .bindFromRequest()
        .fold(
          formWithErrors => {
            personService.getLatestPersons(1, 10).map { indis =>
              BadRequest(lastIndividualView(1, indis, formWithErrors)(using implicitly, implicitly, appConfig))
            }
          },
          formData => {
            journeyCacheRepository.upsert(SelectLatestIndividualForNewFamilyQuestion, formData).map { _ =>
              if (formData.number == -1) {
                Redirect(controllers.add.routes.AddPersonAsPartnerController.searchIndividual)
              } else {
                Redirect(controllers.add.routes.AddPersonAsPartnerController.checkYourAnswers)
              }
            }
          }
        )
  }

  def searchIndividual: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        dbId     <- journeyCacheRepository.get(SelectedDatabaseHidden).map(_.map(_.number).getOrElse(0))
        defaults <- journeyCacheRepository.get(SearchIndividualForNewFamilyQuestion)
      } yield {
        val form = StringForm.stringForm.filledWith(defaults)
        Ok(searchIndividualView(dbId, form)(using implicitly, implicitly))
      }
  }

  def searchIndividualOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(SelectedDatabaseHidden).flatMap {
        case None           => Future.successful(Redirect(controllers.routes.HomeController.onload()))
        case Some(dbIdForm) =>
          StringForm.stringForm
            .bindFromRequest()
            .fold(
              formWithErrors => {
                Future.successful(
                  BadRequest(searchIndividualView(dbIdForm.number, formWithErrors)(using implicitly, implicitly))
                )
              },
              formData => {
                for {
                  _       <- journeyCacheRepository.upsert(SearchIndividualForNewFamilyQuestion, formData)
                  default <- journeyCacheRepository.get(SelectIndividualFromSearch)
                  people  <- personService.searchPersons(dbIdForm.number, formData.value.split("\\s+").toSeq)
                } yield {
                  val form = IntegerForm.integerFormWithLabel.filledWith(default)
                  Ok(
                    searchResultsView(dbIdForm.number, people, form)(
                      using implicitly,
                      implicitly,
                      appConfig
                    )
                  )
                }
              }
            )
      }
  }

  def selectSearchResultsOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        dbIdFormOption        <- journeyCacheRepository.get(SelectedDatabaseHidden)
        searchItemsFormOption <- journeyCacheRepository.get(SearchIndividualForNewFamilyQuestion)
      } yield {
        (dbIdFormOption, searchItemsFormOption) match {
          case (Some(dbIdForm), Some(searchItemsForm)) =>
            IntegerForm.integerFormWithLabel
              .bindFromRequest()
              .fold(
                formWithErrors => {
                  personService.searchPersons(dbIdForm.number, searchItemsForm.value.split("\\s+").toSeq).map {
                    people =>
                      BadRequest(
                        searchResultsView(dbIdForm.number, people, formWithErrors)(
                          using implicitly,
                          implicitly,
                          appConfig
                        )
                      )
                  }
                },
                formData => {
                  journeyCacheRepository.upsert(SelectIndividualFromSearch, formData).map { _ =>
                    Redirect(controllers.add.routes.AddPersonAsPartnerController.checkYourAnswers)
                  }
                }
              )
          case _ => Future.successful(Redirect(controllers.routes.HomeController.onload()))
        }
      }).flatten
  }

  def checkYourAnswers: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        maybeLatestIndividualSelected <- journeyCacheRepository.get(SelectLatestIndividualForNewFamilyQuestion)
        maybeIndividualFromSearch     <- journeyCacheRepository.get(SelectIndividualFromSearch)
        maybeFamilyId                 <- journeyCacheRepository.get(SelectedFamilyIdHidden)
        maybeFamily                   <- maybeFamilyId
          .traverse(id => familyService.getFamilyDetails(id.number, true).value)
          .map(_.flatten)
        maybePerson <- maybeIndividualFromSearch
          .orElse(maybeLatestIndividualSelected)
          .traverse(id => personService.getPerson(id.number, true, true, true))
          .map(_.flatten)
      } yield {
        (maybeFamily, maybePerson) match {
          case (Some(family), Some(person)) =>
            Ok(checkYourAnswersView(person, family)(using implicitly, implicitly, appConfig))
          case _ => BadRequest("Values missing in cache")
        }
      }
  }

  def checkYourAnswersOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        maybeLatestIndividualSelected <- journeyCacheRepository.get(SelectLatestIndividualForNewFamilyQuestion)
        maybeIndividualFromSearch     <- journeyCacheRepository.get(SelectIndividualFromSearch)
        maybeFamilyId                 <- journeyCacheRepository.get(SelectedFamilyIdHidden)
        maybeDatabaseId               <- journeyCacheRepository.get(SelectedDatabaseHidden)
      } yield {
        ((maybeDatabaseId, maybeFamilyId, maybeLatestIndividualSelected, maybeIndividualFromSearch) match {
          case (Some(dbId), Some(familyId), _, Some(individualFromSearch)) =>
            Some((dbId.number, familyId.number, individualFromSearch.number))
          case (Some(dbId), Some(familyId), Some(individualFromLatest), _) if individualFromLatest.number > 0 =>
            Some((dbId.number, familyId.number, individualFromLatest.number))
          case _ => None
        }).fold(Future.successful(BadRequest("Values missing in cache"))) {
          case (dbId, familyId, personId) =>
            updateSqlQueries.updatePartnerFromFamily(personId, familyId).map { _ =>
              Redirect(controllers.routes.FamilyController.showFamily(dbId, familyId))
            }
        }
      }).flatten
  }
}

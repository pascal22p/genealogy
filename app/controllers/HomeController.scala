package controllers

import models.Person

import javax.inject._
import play.api.mvc._
import play.api.i18n._
import services.PersonService
import views.html.Index

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(
  personService: PersonService,
  index: Index,
                                val controllerComponents: ControllerComponents
                              )(
  implicit ec: ExecutionContext
) extends BaseController with I18nSupport {

  def showPerson(id: Int): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    personService.getPerson(id).map { (personOption: Option[Person]) =>
      personOption.fold(NotFound("Nothing here")) { person =>
        Ok(index(person))
      }
    }
  }

  def languageSwitch(lang: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Redirect(routes.HomeController.showPerson(300)).withLang(Lang(lang)))

  }
}

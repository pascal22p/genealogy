package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.data.OptionT
import cats.implicits.*
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.FirstnamesListService
import services.GenealogyDatabaseService
import views.html.FirstnamesList
import views.html.Index
import views.html.SurnamesList

@Singleton
class HomeController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    firstnamesListService: FirstnamesListService,
    indexView: Index,
    surnamesView: SurnamesList,
    firstnamesView: FirstnamesList,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def onload(): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>
    genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
      Ok(indexView(dbs))
    }
  }

  def showSurnames(id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabases.map(_.find(_.id == id)))
        surnames <- OptionT.liftF(genealogyDatabaseService.getSurnamesList(id))
      } yield {
        Ok(surnamesView(surnames, database))
      }).getOrElse(NotFound(s"Genealogy database $id not found"))
  }

  def showFirstnames(id: Int, name: String): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val cursor = request.request.queryString
        .get("cursor")
        .flatMap { s =>
          s.headOption.flatMap { cursorString =>
            cursorString.reverse.split("#", 4).map(_.reverse).toList.reverse match {
              case (name :: id :: birthJd :: deathJd :: _) => Some((name, id.toInt, birthJd.toInt, deathJd.toInt))
              case _                                       => None
            }
          }
        }

      firstnamesListService.getFirstNamesListWithAnchors(id, name, cursor).map { firstnamesListPagination =>
        Ok(
          firstnamesView(
            id,
            name,
            s"Surname $name",
            firstnamesListPagination
          )
        )
      }
  }
}

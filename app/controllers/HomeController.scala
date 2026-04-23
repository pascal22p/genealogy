package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.util.Try

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
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(id))
        surnames <- OptionT.liftF(genealogyDatabaseService.getSurnamesList(id))
      } yield {
        Ok(surnamesView(surnames, Some(database)))
      }).getOrElse(NotFound(s"Genealogy database $id not found"))
  }

  def showFirstnames(id: Int, name: String): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val cursor = request.request.queryString
        .get("cursor")
        .flatMap { s =>
          s.headOption.flatMap { cursorString =>
            cursorString.reverse.split("#", 4).map(_.reverse).toList.reverse match {
              case name :: id :: birthJd :: deathJd :: _ =>
                for {
                  idInt      <- Try(id.toInt).toOption
                  birthJdInt <- Try(birthJd.toInt).toOption
                  deathJdInt <- Try(deathJd.toInt).toOption
                } yield (name, idInt, birthJdInt, deathJdInt)
              case _ => None
            }
          }
        }

      (for {
        database                 <- OptionT(genealogyDatabaseService.getGenealogyDatabase(id))
        firstnamesListPagination <- OptionT.liftF(firstnamesListService.getFirstNamesListWithAnchors(id, name, cursor))
      } yield {
        Ok(
          firstnamesView(
            Some(database),
            name,
            firstnamesListPagination
          )
        )
      }).getOrElse(NotFound(s"Genealogy database $id not found"))
  }
}

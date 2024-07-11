package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.GenealogyDatabaseService
import views.html.FirstnamesList
import views.html.Index
import views.html.SurnamesList

@Singleton
class HomeController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
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
      Ok(indexView(dbs, request.dbId))
    }
  }

  def showSurnames(id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      genealogyDatabaseService.getSurnamesList(id).map { names =>
        Ok(surnamesView(names, request.dbId))
      }
  }

  def showFirstnames(id: Int, name: String): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      genealogyDatabaseService.getFirstnamesList(id, name).map { names =>
        Ok(firstnamesView(names, request.dbId))
      }
  }
}

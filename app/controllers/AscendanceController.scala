package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.*
import cats.data.OptionT
import cats.implicits.*
import models.*
import play.api.i18n.*
import play.api.mvc.*
import services.AscendanceService
import services.GenealogyDatabaseService
import services.SessionService
import utils.TreeUtils
import views.html.AscendantsList
import views.html.AscendantsTree

@Singleton
class AscendanceController @Inject() (
    authAction: AuthAction,
    ascendanceService: AscendanceService,
    sessionService: SessionService,
    genealogyDatabaseService: GenealogyDatabaseService,
    treeUtils: TreeUtils,
    ascendantsList: AscendantsList,
    ascendantsTree: AscendantsTree,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showAscendantList(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        tree     <- OptionT(ascendanceService.getAscendant(id, 1))
      } yield {
        sessionService.insertPersonInHistory(tree.copy(parents = List.empty[Parents]))
        val flattenTree = Map(0 -> List(tree.copy(parents = List.empty[Parents]))) ++ treeUtils.flattenTree(tree)
        val deduplicate = treeUtils.deduplicate(flattenTree)
        Ok(ascendantsList(deduplicate.toList.sortBy(_._1), Some(database)))
      }).getOrElse(NotFound("database or person not found"))
  }

  def showAscendantTree(@unused baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        tree     <- OptionT(ascendanceService.getAscendant(id, 0))
      } yield {
        sessionService.insertPersonInHistory(tree.copy(parents = List.empty[Parents]))
        val flattenTree = Map(0 -> List(tree.copy(parents = List.empty[Parents]))) ++ treeUtils.flattenTree(tree)
        val deduplicate = treeUtils.deduplicate(flattenTree)
        Ok(ascendantsTree(deduplicate.toList.sortBy(_._1), Some(database)))
      }).getOrElse(NotFound("database or person not found"))
  }
}

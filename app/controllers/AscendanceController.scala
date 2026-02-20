package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.*
import cats.implicits.*
import models.*
import play.api.i18n.*
import play.api.mvc.*
import services.AscendanceService
import services.SessionService
import utils.TreeUtils
import views.html.AscendantsList
import views.html.AscendantsTree

@Singleton
class AscendanceController @Inject() (
    authAction: AuthAction,
    ascendanceService: AscendanceService,
    sessionService: SessionService,
    treeUtils: TreeUtils,
    ascendantsList: AscendantsList,
    ascendantsTree: AscendantsTree,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showAscendantList(@unused baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      ascendanceService.getAscendant(id, 0).map {
        case None       => NotFound("Nothing here")
        case Some(tree) =>
          sessionService.insertPersonInHistory(tree.copy(parents = List.empty[Parents]))
          val flattenTree = Map(0 -> List(tree.copy(parents = List.empty[Parents]))) ++ treeUtils.flattenTree(tree)
          val deduplicate = treeUtils.deduplicate(flattenTree)
          Ok(ascendantsList(deduplicate.toList.sortBy(_._1), 1))
      }
  }

  def showAscendantTree(@unused baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      ascendanceService.getAscendant(id, 0).map {
        case None       => NotFound("Nothing here")
        case Some(tree) =>
          sessionService.insertPersonInHistory(tree.copy(parents = List.empty[Parents]))
          val flattenTree = Map(0 -> List(tree.copy(parents = List.empty[Parents]))) ++ treeUtils.flattenTree(tree)
          val deduplicate = treeUtils.deduplicate(flattenTree)
          Ok(ascendantsTree(deduplicate.toList.sortBy(_._1), 1))
      }
  }

}

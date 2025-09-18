package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext

import actions.AuthAction
import config.AppConfig
import models.AuthenticatedRequest
import models.LoggingWithRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.GraphVizDotService
import services.PersonService
import services.Tree
import services.TreeService
import views.html.TreeView

@Singleton
class TreeController @Inject() (
    authAction: AuthAction,
    treeService: TreeService,
    personService: PersonService,
    graphVizDotService: GraphVizDotService,
    treeView: TreeView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext,
    appConfig: AppConfig
) extends BaseController
    with I18nSupport
    with LoggingWithRequest {

  def showTree(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      logger.info("test")
      val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

      personService.getPerson(id, true, true, true).map { maybePerson =>
        maybePerson.fold(NotFound("Person cannot be found")) { person =>
          if (person.details.privacyRestriction.isEmpty || isAllowedToSee) {
            Ok(treeView(baseId, person, 1))
          } else {
            Forbidden("Not allowed to see this person")
          }
        }
      }
  }

  def showSvg(@unused baseId: Int, id: Int, depth: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val tree           = Tree.empty
      val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

      treeService.loadTree(id, 0, depth, tree, isAllowedToSee).map { _ =>
        val dotString = graphVizDotService.treeToDot(tree, id)

        val svg = graphVizDotService.generateImageTree(dotString: String, "svg")
        Ok(svg)
          .as("image/svg+xml")
          .withHeaders(
            "Content-Disposition" -> s"inline; filename=tree_$id.svg"
          )
      }
  }

  def showPdf(@unused baseId: Int, id: Int, depth: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val tree           = Tree.empty
      val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

      treeService.loadTree(id, 0, depth, tree, isAllowedToSee).map { _ =>
        val dotString = graphVizDotService.treeToDot(tree, id)

        val pdf = graphVizDotService.generateImageTree(dotString: String, "pdf")
        Ok(pdf)
          .as("application/pdf")
          .withHeaders(
            "Content-Disposition" -> s"inline; filename=tree_$id.pdf"
          )
      }
  }

  def showPng(@unused baseId: Int, id: Int, depth: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val tree           = Tree.empty
      val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

      treeService.loadTree(id, 0, depth, tree, isAllowedToSee).map { _ =>
        val dotString = graphVizDotService.treeToDot(tree, id)

        val pdf = graphVizDotService.generateImageTree(dotString: String, "png")
        Ok(pdf)
          .as("image/png")
          .withHeaders(
            "Content-Disposition" -> s"inline; filename=tree_$id.png"
          )
      }
  }

  def showDot(@unused baseId: Int, id: Int, depth: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val tree           = Tree.empty
      val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

      treeService.loadTree(id, 0, depth, tree, isAllowedToSee).map { _ =>
        val dotString = graphVizDotService.treeToDot(tree, id)

        Ok(dotString)
          .as("application/txt")
          .withHeaders(
            "Content-Disposition" -> s"inline; filename=tree_$id.dot"
          )
      }
  }

}

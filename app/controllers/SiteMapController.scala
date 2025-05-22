package controllers

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import cats.implicits.*
import play.api.mvc.*
import queries.GetSqlQueries
import services.GenealogyDatabaseService
import views.xml.sitemap.IndexXmlView
import views.xml.sitemap.SitemapXmlView

class SiteMapController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    getSqlQueries: GetSqlQueries,
    indexXmlView: IndexXmlView,
    sitemapXmlView: SitemapXmlView,
    val controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController {

  def sitemapIndex: Action[AnyContent] = Action.async {
    Future.successful(Ok(indexXmlView()).as("application/xml"))
  }

  def sitemapIndividuals: Action[AnyContent] = authAction.async { implicit request =>
    genealogyDatabaseService.getGenealogyDatabases
      .flatMap { databases =>
        databases
          .traverse { database =>
            getSqlQueries.getAllPersonDetails(database.id, None).map { individuals =>
              individuals.map { individual =>
                controllers.routes.IndividualController.showPerson(database.id, individual.id).url
              }
            }
          }
          .map(_.flatten)
      }
      .map { urls =>
        Ok(sitemapXmlView(urls)).as("application/xml")
      }
  }

  def sitemapEvents: Action[AnyContent] = authAction.async { implicit request =>
    getSqlQueries.getAllEvents
      .map { events =>
        events.map { event =>
          controllers.routes.EventController.showEvent(event.base, event.events_details_id).url
        }
      }
      .map { urls =>
        Ok(sitemapXmlView(urls)).as("application/xml")
      }
  }

  def sitemapFirstnames: Action[AnyContent] = authAction.async { implicit request =>
    genealogyDatabaseService.getGenealogyDatabases
      .flatMap { databases =>
        databases
          .traverse { database =>
            genealogyDatabaseService.getSurnamesList(database.id).map { names =>
              names.map { name =>
                controllers.routes.HomeController.showFirstnames(database.id, name.name).url
              }
            }
          }
          .map(_.flatten)
      }
      .map { urls =>
        Ok(sitemapXmlView(urls)).as("application/xml")
      }
  }

  def sitemapSurnames: Action[AnyContent] = authAction.async { implicit request =>
    genealogyDatabaseService.getGenealogyDatabases
      .map { databases =>
        databases
          .map { database =>
            controllers.routes.HomeController.showSurnames(database.id).url
          }
      }
      .map { urls =>
        Ok(sitemapXmlView(urls)).as("application/xml")
      }
  }

}

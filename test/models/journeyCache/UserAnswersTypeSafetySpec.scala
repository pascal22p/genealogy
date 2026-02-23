package models.journeyCache

import scala.compiletime.testing.typeCheckErrors
import scala.compiletime.testing.Error

import org.scalatest.matchers.should.Matchers._
import testUtils.BaseSpec

class UserAnswersTypeSafetySpec extends BaseSpec {

  "upsert rejects wrong type" ignore {
    // this does not work in a github action runner environment

    val errors: List[Error] = typeCheckErrors("""
        import models.forms.{GedcomListForm, NewDatabaseForm}
        import models.journeyCache.UserAnswersKey.GedcomPath
        import repositories.MariadbJourneyCacheRepository
        import models.journeyCache.{UserAnswersItem, UserAnswersKey}

        val repo: MariadbJourneyCacheRepository = ???
        repo.upsert(GedcomPath, NewDatabaseForm("", ""))(???, ???)
      """)

    withClue(errors.mkString("\n")) {
      assert(errors.exists(_.message.contains("Found:    models.forms.NewDatabaseForm")))
      assert(errors.exists(_.message.contains("Required: models.forms.GedcomListForm")))
    }
  }

  "upsert rejects wrong type bis" in {
    """
          import models.forms.{GedcomListForm, CreateNewDatabaseForm}
          import models.journeyCache.UserAnswersKey.ChooseGedcomFileQuestion
          import repositories.MariadbJourneyCacheRepository
          import models.journeyCache.{UserAnswersItem, UserAnswersKey}

          val repo: MariadbJourneyCacheRepository = ???
          repo.upsert(ChooseGedcomFileQuestion, CreateNewDatabaseForm("", ""))(???, ???)
        """ shouldNot compile
  }
}

package models.journeyCache

import scala.compiletime.testing.typeCheckErrors
import scala.compiletime.testing.Error

import org.scalatest.matchers.should.Matchers._
import testUtils.BaseSpec

class UserAnswersTypeSafetySpec extends BaseSpec {

  "upsert rejects wrong type using scala.compileTime" in {
    val errors: List[Error] = typeCheckErrors("""
        import models.forms.{GedcomListForm, CreateNewDatabaseForm}
        import models.journeyCache.UserAnswersKey.ChooseGedcomFileQuestion
        import repositories.MariadbJourneyCacheRepository
        import models.journeyCache.{UserAnswersItem, UserAnswersKey}

        val repo: MariadbJourneyCacheRepository = ???
        repo.upsert(ChooseGedcomFileQuestion, CreateNewDatabaseForm("", ""))(???, ???)
      """)

    withClue(errors.mkString("\n")) {
      assert(errors.exists(_.message.contains("Found:    models.forms.CreateNewDatabaseForm")))
      assert(errors.exists(_.message.contains("Required: models.forms.GedcomPathInputTextForm")))
    }
  }

  "upsert rejects wrong type using scalatest" in {
    """
        import models.forms.{GedcomListForm, CreateNewDatabaseForm}
        import models.journeyCache.UserAnswersKey.ChooseGedcomFileQuestion
        import repositories.MariadbJourneyCacheRepository
        import models.journeyCache.{UserAnswersItem, UserAnswersKey}

        val repo: MariadbJourneyCacheRepository = ???
        repo.upsert(ChooseGedcomFileQuestion, CreateNewDatabaseForm("", ""))(???, ???)
      """ shouldNot typeCheck
  }
}

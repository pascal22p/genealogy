package models.journeyCache

import scala.compiletime.testing.typeCheckErrors
import scala.compiletime.testing.Error

import testUtils.BaseSpec

class UserAnswersTypeSafetySpec extends BaseSpec {

  "upsert rejects wrong type" in {
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

}

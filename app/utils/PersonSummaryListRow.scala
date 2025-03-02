package utils

import javax.inject.Inject

import config.AppConfig
import models.AuthenticatedRequest
import models.Person
import models.ResnType.PrivacyResn
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value
import uk.gov.hmrc.govukfrontend.views.Aliases.ActionItem
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class PersonSummaryListRow @Inject() ()(implicit val appConfig: AppConfig) {

  def personSummaryListRow(
      dbId: Int,
      person: Person
  )(implicit messages: Messages, authenticatedRequest: AuthenticatedRequest[?]): SummaryListRow = {
    val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

    if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
      SummaryListRow(
        key = Key(Text(person.details.shortName)),
        value = Value(HtmlContent(person.events.birthAndDeathDate)),
        classes = "govuk-summary-list__row--no-border",
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = controllers.routes.IndividualController.showPerson(dbId, person.details.id).url,
                content = Text("View")
              ),
              ActionItem(
                href = controllers.delete.routes.DeleteIndividualController
                  .deletePersonConfirmation(dbId, person.details.id)
                  .url,
                content = Text("Delete")
              )
            )
          )
        )
      )
    } else {
      SummaryListRow(
        key = Key(Text(appConfig.redactedMask)),
        value = Value(HtmlContent(appConfig.redactedMask)),
        classes = "govuk-summary-list__row--no-border",
        actions = None
      )
    }
  }

}

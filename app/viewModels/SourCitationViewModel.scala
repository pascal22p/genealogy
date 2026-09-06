package viewModels

import models.SourCitationQueryData

final case class SourCitationUsageViewModel(
    owner: Option[HtmlLinkViewModel],
    persons: List[HtmlLinkViewModel]
)

final case class SourCitationViewModel(
    sourCitation: SourCitationQueryData,
    usages: List[SourCitationUsageViewModel]
)

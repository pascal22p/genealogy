package models

import models.queryData.FirstnameWithBirthDeath

final case class FirstnamesListPagination(
    firstnames: Seq[FirstnameWithBirthDeath],
    current: Option[Cursor],
    previous: Seq[Cursor],
    next: Seq[Cursor],
    first: Option[Cursor],
    last: Option[Cursor]
)

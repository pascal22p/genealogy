package utils

import java.time.LocalDate

import testUtils.BaseSpec

class GedcomDateLibrarySpec extends BaseSpec {
  "GedcomDateLibrary" must {
    "correctly parse a valid Gregorian date" in {
      val date         = "15 APR 2021"
      val expectedDate = LocalDate.of(2021, 4, 15)
      GedcomDateLibrary.extractDate(date) mustBe Some(expectedDate)
    }

    "correctly parse a valid Gregorian date with only year" in {
      val date         = "2021"
      val expectedDate = LocalDate.of(2021, 1, 1)
      GedcomDateLibrary.extractDate(date) mustBe Some(expectedDate)
    }

    "correctly parse a valid Gregorian date with month and year" in {
      val date         = "APR 2021"
      val expectedDate = LocalDate.of(2021, 4, 1)
      GedcomDateLibrary.extractDate(date) mustBe Some(expectedDate)
    }

    "return None for an invalid date" in {
      val date = "invalid date"
      GedcomDateLibrary.extractDate(date) mustBe None
    }

  }
}

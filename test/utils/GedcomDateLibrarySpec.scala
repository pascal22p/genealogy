package utils

import java.time.LocalDate

import testUtils.BaseSpec

class GedcomDateLibrarySpec extends BaseSpec {
  val republicanDate = new RepublicanDate()
  val sut            = new GedcomDateLibrary(republicanDate)

  "GedcomDateLibrary" must {
    "correctly parse a valid Gregorian date" in {
      val date         = "15 APR 2021"
      val expectedDate = LocalDate.of(2021, 4, 15)
      sut.extractDate(date) mustBe Some(expectedDate)
    }

    "correctly parse a valid Gregorian date with only year" in {
      val date         = "2021"
      val expectedDate = LocalDate.of(2021, 1, 1)
      sut.extractDate(date) mustBe Some(expectedDate)
    }

    "correctly parse a valid Gregorian date with month and year" in {
      val date         = "APR 2021"
      val expectedDate = LocalDate.of(2021, 4, 1)
      sut.extractDate(date) mustBe Some(expectedDate)
    }

    "return None for an invalid date" in {
      val date = "invalid date"
      sut.extractDate(date) mustBe None
    }

  }
}

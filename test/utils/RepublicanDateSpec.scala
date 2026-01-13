package utils

import java.time.LocalDate

import testUtils.BaseSpec

class RepublicanDateSpec extends BaseSpec {

  "romanToInt" must {
    "convert I to 1" in {
      RepublicanDate.romanToInt("I") mustBe 1
    }

    "convert II to 2" in {
      RepublicanDate.romanToInt("II") mustBe 2
    }

    "convert III to 3" in {
      RepublicanDate.romanToInt("III") mustBe 3
    }

    "convert IV to 4" in {
      RepublicanDate.romanToInt("IV") mustBe 4
    }

    "convert V to 5" in {
      RepublicanDate.romanToInt("V") mustBe 5
    }

    "convert VI to 6" in {
      RepublicanDate.romanToInt("VI") mustBe 6
    }

    "convert IX to 9" in {
      RepublicanDate.romanToInt("IX") mustBe 9
    }

    "convert X to 10" in {
      RepublicanDate.romanToInt("X") mustBe 10
    }

    "convert XI to 11" in {
      RepublicanDate.romanToInt("XI") mustBe 11
    }

    "convert XIV to 14" in {
      RepublicanDate.romanToInt("XIV") mustBe 14
    }
  }

  "parseRepublicanDate" must {
    "parse exact date with numeric year" in {
      RepublicanDate.parseRepublicanDate("1 VEND 1") mustBe Some((1, "VEND", 1))
    }

    "parse exact date with roman year" in {
      RepublicanDate.parseRepublicanDate("1 VEND I") mustBe Some((1, "VEND", 1))
    }

    "parse month and numeric year" in {
      RepublicanDate.parseRepublicanDate("VEND 1") mustBe Some((1, "VEND", 1))
    }

    "parse month and roman year" in {
      RepublicanDate.parseRepublicanDate("VEND I") mustBe Some((1, "VEND", 1))
    }

    "parse numeric year only" in {
      RepublicanDate.parseRepublicanDate("1") mustBe Some((1, "VEND", 1))
    }

    "parse roman year only" in {
      RepublicanDate.parseRepublicanDate("I") mustBe Some((1, "VEND", 1))
    }

    "return None for invalid date" in {
      RepublicanDate.parseRepublicanDate("invalid date") mustBe None
    }
  }

  "fromRepublicanToGregorian" must {
    "convert 1 VEND 1 to 1792-09-22" in {
      RepublicanDate.fromRepublicanToGregorian(1, "VEND", 1) mustBe Some(LocalDate.of(1792, 9, 22))
    }

    "convert 1 BRUM 1 to 1792-10-22" in {
      RepublicanDate.fromRepublicanToGregorian(1, "BRUM", 1) mustBe Some(LocalDate.of(1792, 10, 22))
    }

    "convert 1 FRIM 1 to 1792-11-21" in {
      RepublicanDate.fromRepublicanToGregorian(1, "FRIM", 1) mustBe Some(LocalDate.of(1792, 11, 21))
    }

    "convert 1 NIVO 1 to 1792-12-21" in {
      RepublicanDate.fromRepublicanToGregorian(1, "NIVO", 1) mustBe Some(LocalDate.of(1792, 12, 21))
    }

    "convert 1 PLUV 1 to 1793-01-20" in {
      RepublicanDate.fromRepublicanToGregorian(1, "PLUV", 1) mustBe Some(LocalDate.of(1793, 1, 20))
    }

    "convert 1 VENT 1 to 1793-02-19" in {
      RepublicanDate.fromRepublicanToGregorian(1, "VENT", 1) mustBe Some(LocalDate.of(1793, 2, 19))
    }

    "convert 1 GERM 1 to 1793-03-21" in {
      RepublicanDate.fromRepublicanToGregorian(1, "GERM", 1) mustBe Some(LocalDate.of(1793, 3, 21))
    }

    "convert 1 FLOR 1 to 1793-04-20" in {
      RepublicanDate.fromRepublicanToGregorian(1, "FLOR", 1) mustBe Some(LocalDate.of(1793, 4, 20))
    }

    "convert 1 PRAI 1 to 1793-05-20" in {
      RepublicanDate.fromRepublicanToGregorian(1, "PRAI", 1) mustBe Some(LocalDate.of(1793, 5, 20))
    }

    "convert 1 MESS 1 to 1793-06-19" in {
      RepublicanDate.fromRepublicanToGregorian(1, "MESS", 1) mustBe Some(LocalDate.of(1793, 6, 19))
    }

    "convert 1 THER 1 to 1793-07-19" in {
      RepublicanDate.fromRepublicanToGregorian(1, "THER", 1) mustBe Some(LocalDate.of(1793, 7, 19))
    }

    "convert 1 FRUC 1 to 1793-08-18" in {
      RepublicanDate.fromRepublicanToGregorian(1, "FRUC", 1) mustBe Some(LocalDate.of(1793, 8, 18))
    }

    "convert 10 VEND 1 to 1792-10-01" in {
      RepublicanDate.fromRepublicanToGregorian(10, "VEND", 1) mustBe Some(LocalDate.of(1792, 10, 1))
    }

    "convert 15 BRUM 1 to 1792-11-05" in {
      RepublicanDate.fromRepublicanToGregorian(15, "BRUM", 1) mustBe Some(LocalDate.of(1792, 11, 5))
    }

    "convert 1 COMP 1 to 1793-09-17" in {
      RepublicanDate.fromRepublicanToGregorian(1, "COMP", 1) mustBe Some(LocalDate.of(1793, 9, 17))
    }

    (1 until 14).foreach { year =>
      val offset =
        if (year == 12) 2
        else if (year == 4 || year >= 8) 1
        else 0
      s"convert 12 FRIM $year to ${year + 1791}-12-${2 + offset}" in {
        RepublicanDate.fromRepublicanToGregorian(12, "FRIM", year) mustBe Some(
          LocalDate.of(year + 1791, 12, 2 + offset)
        )
      }
    }
  }

}

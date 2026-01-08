package testUtils

import java.nio.file.Paths

import testUtils.BaseSpec
import utils.FileUtils

class FileUtilsSpec extends BaseSpec {
  private val gedcomBasePath: String = "test/resources/gedcom/"

  "detectGedcomCharset" must {
    "detect ANSEL charset" in {
      val charset = FileUtils.detectGedcomCharset(Paths.get(gedcomBasePath + "TGC551LF.ged"))
      charset.name mustBe "ISO-8859-1"
    }
  }

  "ReadGedcomAsString" must {
    "import ansel correctly" in {
      val expected = """0 HEAD
                       |1 SOUR sbt test
                       |1 GEDC
                       |2 VERS 5.5
                       |2 FORM LINEAGE-LINKED
                       |1 CHAR ANSEL
                       |0 @20060I@ INDI
                       |1 NAME é è ê ë ó ò ô ö /SURNAME/
                       |""".stripMargin
      val gedcom = FileUtils.readGedcomAsString(gedcomBasePath + "ansel.ged")
      gedcom mustBe expected
    }
  }

}

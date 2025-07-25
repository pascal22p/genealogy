package services.gedcom

import cats.data.Ior
import models.gedcom.GedComPersonalNameStructure
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode
import testUtils.BaseSpec

class GedcomIndividualParserServiceSpec extends BaseSpec {

  val gedcomHashIdTable = new GedcomHashIdTable
  val gedcomEventParser = new GedcomEventParser(gedcomHashIdTable)
  val sut               = new GedcomIndividualParser(
    new GedcomCommonParser,
    gedcomHashIdTable,
    gedcomEventParser
  )

  val gedcomString: String = """
                               |0 HEAD
                               |1 SOUR webtreeprint.com
                               |2 VERS 1.0
                               |2 NAME webtreeprint
                               |1 DATE 25 OCT 2012
                               |1 FILE bronte.ged
                               |1 GEDC
                               |2 VERS 5.5
                               |2 FORM LINEAGE-LINKED
                               |1 CHAR UTF-8
                               |1 SUBM @SUB1@
                               |0 @SUB1@ SUBM
                               |1 NAME webTreePrint
                               |0 @I0001@ INDI
                               |1 NAME Patrick /Brontë/
                               |2 GIVN Patrick
                               |2 SURN Brontë
                               |1 SEX M
                               |1 BIRT
                               |2 DATE 17 MAR 1777
                               |2 PLAC County Down, Ireland
                               |1 DEAT
                               |2 DATE 7 JUN 1861
                               |2 PLAC Haworth, Yorks.
                               |1 ALIA Brunty
                               |1 FAMC @F003@
                               |1 FAMS @F001@
                               |0 @I0002@ INDI
                               |1 NAME Maria /Branwell/
                               |2 GIVN Maria
                               |2 SURN Branwell
                               |1 SEX F
                               |1 BIRT
                               |2 DATE 15 APR 1783
                               |2 PLAC Penzance, Cornwall
                               |1 DEAT
                               |2 DATE 15 SEP 1821
                               |1 FAMC @F004@
                               |1 FAMS @F001@
                               |0 @I0003@ INDI
                               |1 NAME Maria /Brontë/
                               | 2 GIVN Maria
                               | 2 SURN Brontë
                               | 1 SEX F
                               | 1 BIRT
                               | 2 DATE 23 APR 1814
                               |  2 PLAC Clough House, High Town
                               |  1 DEAT
                               | 2 DATE 6 MAY 1825
                               |   2 PLAC Howarth
                               |  1 FAMC @F001@
                               |   0 @I0004@ INDI
                               |  1 NAME Elizabeth /Brontë/
                               |  2 GIVN Elizabeth
                               |2 SURN Brontë
                               |1 SEX F
                               | 1 BIRT
                               |2 DATE 8 FEB 1815
                               |  1 DEAT
                               | 2 DATE 15 JUN 1825
                               | 2 PLAC Howarth
                               |1 FAMC @F001@
                               | 0 @I0005@ INDI
                               |1 NAME Charlotte /Brontë/
                               |2 GIVN Charlotte
                               |2 SURN Brontë
                               |1 SEX F
                               |1 BIRT
                               |2 DATE 21 APR 1816
                               |2 PLAC Thornton, Nr. Bradford
                               |1 DEAT
                               |2 DATE 31 MAR 1855
                               |2 PLAC Howarth
                               |1 FAMC @F001@
                               |1 FAMS @F002@
                               |0 @I0006@ INDI
                               |1 NAME Patrick Branwell /Brontë/
                               |2 GIVN Patrick Branwell
                               |2 SURN Brontë
                               |1 SEX M
                               |1 BIRT
                               |2 DATE 26 JUN 1817
                               |2 PLAC Thornton, Nr. Bradford
                               |1 DEAT
                               |2 DATE 24 SEP 1848
                               |2 PLAC Howarth
                               |1 FAMC @F001@
                               |0 @I0007@ INDI
                               |1 NAME Emily Jane /Brontë/
                               |2 GIVN Emily Jane
                               |2 SURN Brontë
                               |1 SEX F
                               |1 BIRT
                               |2 DATE 30 JUL 1818
                               |2 PLAC Thornton, Nr. Bradford
                               |1 DEAT
                               |2 DATE 19 DEC 1848
                               |2 PLAC Howarth
                               |1 FAMC @F001@
                               |0 @I0008@ INDI
                               |1 NAME Anne /Brontë/
                               |2 GIVN Anne
                               |2 SURN Brontë
                               |1 SEX F
                               |1 BIRT
                               |2 DATE 17 JAN 1820
                               |2 PLAC Thornton, Nr. Bradford
                               |1 DEAT
                               |2 DATE 28 MAY 1849
                               |2 PLAC Scarborough
                               |1 FAMC @F001@
                               |0 @I0009@ INDI
                               |1 NAME Arthur Bell /Nicholls/
                               |2 GIVN Arthur Bell
                               |2 SURN Nicholls
                               |1 SEX M
                               |1 FAMS @F002@
                               |0 @I0010@ INDI
                               |1 NAME Eleanor /McClory/
                               |2 GIVN Eleanor
                               |2 SURN McClory
                               |1 SEX F
                               |1 FAMS @F003@
                               |0 @I0011@ INDI
                               |1 NAME Hugh /Brunty/
                               |2 GIVN Hugh
                               |2 SURN Brunty
                               |1 SEX M
                               |1 BIRT
                               |2 DATE 1755
                               |1 DEAT
                               |2 DATE abt 1808
                               |1 FAMS @F003@
                               |0 @I0012@ INDI
                               |1 NAME Anne /Carne/
                               |2 GIVN Anne
                               |2 SURN Carne
                               |1 SEX F
                               |1 BIRT
                               |2 DATE APR 1744
                               |1 DEAT
                               |2 DATE 19 DEC 1809
                               |1 FAMS @F004@
                               |0 @I0013@ INDI
                               |1 NAME Thomas /Branwell/
                               |2 GIVN Thomas
                               |2 SURN Branwell
                               |1 SEX M
                               |1 BIRT
                               |2 DATE 1746
                               |1 DEAT
                               |2 DATE 5 APR 1808
                               |1 FAMS @F004@
                               |0 @I0014@ INDI
                               |1 NAME Elizabeth /Branwell/
                               |2 GIVN Elizabeth
                               |2 SURN Branwell
                               |2 NICK Aunt
                               |1 SEX F
                               |1 BIRT
                               |2 DATE 1776
                               |1 DEAT
                               |2 DATE 29 OCT 1842
                               |1 FAMC @F004@
                               |0 @F001@ FAM
                               |1 HUSB @I0001@
                               |1 WIFE @I0002@
                               |1 MARR
                               |2 DATE 29 December 1812
                               |1 CHIL @I0003@
                               |1 CHIL @I0004@
                               |1 CHIL @I0005@
                               |1 CHIL @I0006@
                               |1 CHIL @I0007@
                               |1 CHIL @I0008@
                               |0 @F002@ FAM
                               |1 HUSB @I0009@
                               |1 WIFE @I0005@
                               |1 MARR
                               |2 DATE 29 JUN 1854
                               |0 @F003@ FAM
                               |1 HUSB @I0011@
                               |1 WIFE @I0010@
                               |1 MARR
                               |2 DATE 1776
                               |1 CHIL @I0001@
                               |0 @F004@ FAM
                               |1 HUSB @I0013@
                               |1 WIFE @I0012@
                               |1 MARR
                               |2 DATE 1768
                               |1 CHIL @I0002@
                               |1 CHIL @I0014@
                               |0 TRLR
                               |""".stripMargin

  "readPersonalNameStructure" must {
    "parse a name structure with no warning" in {
      val input = GedcomNode(
        "NAME",
        "1 NAME Firstname /Surname/",
        0,
        1,
        None,
        Some("Firstname /Surname/"),
        List(
          GedcomNode("NPFX", "2 NPFX prefix", 1, 2, None, Some("prefix"), List()),
          GedcomNode("GIVN", "2 GIVN given", 2, 2, None, Some("given"), List()),
          GedcomNode("NICK", "2 NICK nickname", 3, 2, None, Some("nickname"), List()),
          GedcomNode("SPFX", "2 SPFX suffix", 4, 2, None, Some("suffix"), List()),
          GedcomNode("SURN", "2 SURN Surname", 5, 2, None, Some("Surname"), List()),
          GedcomNode("NSFX", "2 NSFX surname prefix", 6, 2, None, Some("surname prefix"), List()),
          GedcomNode(
            "FON",
            "2 FON not supported",
            7,
            2,
            None,
            Some("Surname"),
            List(
              GedcomNode("TYPE", "3 SURN Surname", 8, 3, None, Some("Surname"), List())
            )
          )
        )
      )

      val expected = Ior.Both(
        List("Line 7: `2 FON not supported` is not supported"),
        GedComPersonalNameStructure(
          "Firstname /Surname/",
          "prefix",
          "given",
          "nickname",
          "suffix",
          "surname prefix",
          "surn"
        )
      )

      val result: Ior[List[String], GedComPersonalNameStructure] = sut.readPersonalNameStructure(input)
      result mustBe expected
    }
  }

  "readIndiBlock" must {
    "parse a INDI structure with no warning" in {
      /*
      """0 @I626@ INDI
           |1 NAME Caroline of_Baden //
           |2 GIVN given
           |2 NICK nickname
           |1 SEX F
           |1 BIRT
           |2 DATE 13 JUL 1776
           |2 PLAC Karlsruhe
           |1 DEAT
           |2 DATE 13 NOV 1841
           |2 PLAC Munich,Germany
           |1 BURI
           |2 PLAC Theatinerkirche,Munich,Germany
           |1 FAMS @F232@
           |1 FAMC @F226@
           |""".stripMargin
       */

      val input = GedcomNode(
        "INDI",
        "0 @I626@ INDI",
        0,
        0,
        Some("I626"),
        None,
        List(
          GedcomNode(
            "NAME",
            "1 NAME Caroline of_Baden //",
            1,
            1,
            None,
            Some("Caroline of_Baden //"),
            List(
              GedcomNode("GIVN", "2 GIVN given", 2, 2, None, Some("given"), List()),
              GedcomNode("NICK", "2 NICK nickname", 3, 2, None, Some("nickname"), List()),
            )
          ),
          GedcomNode("SEX", "1 SEX F", 2, 1, None, Some("F"), List()),
          GedcomNode(
            "BIRT",
            "1 BIRT",
            3,
            1,
            None,
            None,
            List(
              GedcomNode("DATE", "2 DATE 13 JUL 1776", 3, 2, None, Some("13 JUL 1776"), List()),
              GedcomNode("PLAC", "2 PLAC Karlsruhe", 4, 2, None, Some("Karlsruhe"), List())
            )
          ),
          GedcomNode(
            "DEAT",
            "1 DEAT",
            6,
            1,
            None,
            None,
            List(
              GedcomNode("DATE", "2 DATE 13 NOV 1841", 6, 2, None, Some("13 NOV 1841"), List()),
              GedcomNode("PLAC", "2 PLAC Munich,Germany", 7, 2, None, Some("Munich,Germany"), List())
            )
          ),
          GedcomNode(
            "BURI",
            "1 BURI",
            9,
            1,
            None,
            None,
            List(
              GedcomNode(
                "PLAC",
                "2 PLAC Theatinerkirche,Munich,Germany",
                9,
                2,
                None,
                Some("Theatinerkirche,Munich,Germany"),
                List()
              )
            )
          ),
          GedcomNode("FAMS", "1 FAMS @F232@", 11, 1, Some("F232"), None, List()),
          GedcomNode("FAMC", "1 FAMC @F226@", 12, 1, Some("F226"), None, List())
        )
      )

      val expected = Ior.Both(
        List(
          "Line 4: `2 PLAC Karlsruhe` is not supported",
          "Line 7: `2 PLAC Munich,Germany` is not supported",
          "Line 9: `2 PLAC Theatinerkirche,Munich,Germany` is not supported",
          "Line 11: `1 FAMS @F232@` is not supported",
          "Line 12: `1 FAMC @F226@` is not supported"
        ),
        GedcomIndiBlock(
          GedComPersonalNameStructure("Caroline of_Baden //", "", "given", "nickname", "", "", "surn"),
          None,
          "F",
          0,
          List(
            GedcomEventBlock("BIRT", "13 JUL 1776"),
            GedcomEventBlock("DEAT", "13 NOV 1841"),
            GedcomEventBlock("BURI", "")
          )
        )
      )

      val result = sut.readIndiBlock(input)
      result mustBe expected
    }
  }

}

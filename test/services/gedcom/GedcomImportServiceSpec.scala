package services.gedcom

import scala.concurrent.Future

import anorm.ResultSetParser
import anorm.SQL
import anorm.SqlParser.int
import testUtils.MariadbHelper

class GedcomImportServiceSpec extends MariadbHelper {

  lazy val sut: GedcomImportService = app.injector.instanceOf[GedcomImportService]

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

  "gedcom2sql" must {
    "works" in {
      val parser: ResultSetParser[Option[Int]] = {
        int("insert_id").singleOpt
      }

      val result = for {
        _ <- Future {
          db.withConnection { implicit conn =>
            SQL("SET FOREIGN_KEY_CHECKS=1;")
            SQL(
              "INSERT INTO `genea_infos` (`nom`, `descriptif`, `entetes`, `ged_corp`, `subm`) VALUES ({name}, '', '', '', NULL)"
            )
              .on("name" -> "test")
              .executeInsert[Option[Int]](parser)
          }
        }
        result <- sut.gedcom2sql(gedcomString, 1)
      } yield result

      result.futureValue mustBe false
    }
  }
}

package services.gedcom

import java.time.LocalDateTime

import scala.concurrent.Future

import anorm.ResultSetParser
import anorm.Row
import anorm.RowParser
import anorm.SQL
import anorm.SqlParser
import anorm.SqlParser.int
import anorm.SqlStringInterpolation
import anorm.Success
import models.forms.PlacesElementsForm
import models.forms.PlacesElementsPaddingForm
import models.forms.PlacesElementsSeparatorForm
import models.gedcom.PlaceSubdivisionMapping
import models.journeyCache.UserAnswersKey.PlacesElementsPaddingQuestion
import models.journeyCache.UserAnswersKey.PlacesElementsQuestion
import models.journeyCache.UserAnswersKey.PlacesElementsSeparatorQuestion
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import models.UserData
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.scalatest.AppendedClues.convertToClueful
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testUtils.MariadbHelper

class GedcomImportServiceBronteSpec extends MariadbHelper {

  lazy val sut: GedcomImportService = app.injector.instanceOf[GedcomImportService]

  val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      FakeRequest(),
      Session("", SessionData(Some(UserData(0, "", "", false, false))), LocalDateTime.now),
      None
    )

  val expectedOutput: Map[String, String] = Map(
    "rel_familles_indi" -> """(indi_id = 3, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 4, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 5, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 6, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 7, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 8, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 1, familles_id = 2, rela_type = , rela_stat = None)
                             |(indi_id = 2, familles_id = 3, rela_type = , rela_stat = None)
                             |(indi_id = 14, familles_id = 3, rela_type = , rela_stat = None)""".stripMargin,
    "rel_indi_events" -> """(indi_id = 1, events_tag = BIRT, events_attestation = None, events_details_id = 1, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 1, events_tag = DEAT, events_attestation = None, events_details_id = 2, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 2, events_tag = BIRT, events_attestation = None, events_details_id = 3, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 2, events_tag = DEAT, events_attestation = None, events_details_id = 4, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 3, events_tag = BIRT, events_attestation = None, events_details_id = 5, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 3, events_tag = DEAT, events_attestation = None, events_details_id = 6, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 4, events_tag = BIRT, events_attestation = None, events_details_id = 7, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 4, events_tag = DEAT, events_attestation = None, events_details_id = 8, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 5, events_tag = BIRT, events_attestation = None, events_details_id = 9, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 5, events_tag = DEAT, events_attestation = None, events_details_id = 10, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 6, events_tag = BIRT, events_attestation = None, events_details_id = 11, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 6, events_tag = DEAT, events_attestation = None, events_details_id = 12, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 7, events_tag = BIRT, events_attestation = None, events_details_id = 13, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 7, events_tag = DEAT, events_attestation = None, events_details_id = 14, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 8, events_tag = BIRT, events_attestation = None, events_details_id = 15, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 8, events_tag = DEAT, events_attestation = None, events_details_id = 16, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 11, events_tag = BIRT, events_attestation = None, events_details_id = 17, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 11, events_tag = DEAT, events_attestation = None, events_details_id = 18, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BIRT, events_attestation = None, events_details_id = 19, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = DEAT, events_attestation = None, events_details_id = 20, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 13, events_tag = BIRT, events_attestation = None, events_details_id = 21, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 13, events_tag = DEAT, events_attestation = None, events_details_id = 22, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 14, events_tag = BIRT, events_attestation = None, events_details_id = 23, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 14, events_tag = DEAT, events_attestation = None, events_details_id = 24, timestamp = xxxx-xx-xx xx:xx:xx)""".stripMargin,
    "genea_events_details" -> """(place_id = Some(4), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 17 MAR 1777, events_details_descriptor = , events_details_id = 1, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(1), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 7 JUN 1861, events_details_descriptor = , events_details_id = 2, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(6), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 APR 1783, events_details_descriptor = , events_details_id = 3, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 SEP 1821, events_details_descriptor = , events_details_id = 4, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(3), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 23 APR 1814, events_details_descriptor = , events_details_id = 5, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(7), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 6 MAY 1825, events_details_descriptor = , events_details_id = 6, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 8 FEB 1815, events_details_descriptor = , events_details_id = 7, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(7), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 JUN 1825, events_details_descriptor = , events_details_id = 8, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 21 APR 1816, events_details_descriptor = , events_details_id = 9, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(7), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 MAR 1855, events_details_descriptor = , events_details_id = 10, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 26 JUN 1817, events_details_descriptor = , events_details_id = 11, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(7), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 24 SEP 1848, events_details_descriptor = , events_details_id = 12, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 30 JUL 1818, events_details_descriptor = , events_details_id = 13, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(7), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 19 DEC 1848, events_details_descriptor = , events_details_id = 14, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 17 JAN 1820, events_details_descriptor = , events_details_id = 15, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(5), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 28 MAY 1849, events_details_descriptor = , events_details_id = 16, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1755, events_details_descriptor = , events_details_id = 17, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = abt 1808, events_details_descriptor = , events_details_id = 18, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = APR 1744, events_details_descriptor = , events_details_id = 19, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 19 DEC 1809, events_details_descriptor = , events_details_id = 20, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1746, events_details_descriptor = , events_details_id = 21, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 5 APR 1808, events_details_descriptor = , events_details_id = 22, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1776, events_details_descriptor = , events_details_id = 23, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 29 OCT 1842, events_details_descriptor = , events_details_id = 24, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )""".stripMargin,
    "genea_individuals" -> """(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Brontë, indi_resn = None, indi_prenom = Patrick, indi_id = 1, indi_spfx = , indi_givn = Patrick)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Branwell, indi_resn = None, indi_prenom = Maria, indi_id = 2, indi_spfx = , indi_givn = Maria)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Brontë, indi_resn = None, indi_prenom = Maria, indi_id = 3, indi_spfx = , indi_givn = Maria)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Brontë, indi_resn = None, indi_prenom = Elizabeth, indi_id = 4, indi_spfx = , indi_givn = Elizabeth)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Brontë, indi_resn = None, indi_prenom = Charlotte, indi_id = 5, indi_spfx = , indi_givn = Charlotte)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Brontë, indi_resn = None, indi_prenom = Patrick Branwell, indi_id = 6, indi_spfx = , indi_givn = Patrick Branwell)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Brontë, indi_resn = None, indi_prenom = Emily Jane, indi_id = 7, indi_spfx = , indi_givn = Emily Jane)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Brontë, indi_resn = None, indi_prenom = Anne, indi_id = 8, indi_spfx = , indi_givn = Anne)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Nicholls, indi_resn = None, indi_prenom = Arthur Bell, indi_id = 9, indi_spfx = , indi_givn = Arthur Bell)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = McClory, indi_resn = None, indi_prenom = Eleanor, indi_id = 10, indi_spfx = , indi_givn = Eleanor)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Brunty, indi_resn = None, indi_prenom = Hugh, indi_id = 11, indi_spfx = , indi_givn = Hugh)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Carne, indi_resn = None, indi_prenom = Anne, indi_id = 12, indi_spfx = , indi_givn = Anne)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Branwell, indi_resn = None, indi_prenom = Thomas, indi_id = 13, indi_spfx = , indi_givn = Thomas)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = Aunt, base = 1, indi_sexe = F, indi_nom = Branwell, indi_resn = None, indi_prenom = Elizabeth, indi_id = 14, indi_spfx = , indi_givn = Elizabeth)""".stripMargin,
    "genea_familles" -> """(familles_resn = None, familles_husb = Some(1), familles_refn = , familles_refn_type = , familles_wife = Some(2), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 1)
                          |(familles_resn = None, familles_husb = Some(11), familles_refn = , familles_refn_type = , familles_wife = Some(10), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 2)
                          |(familles_resn = None, familles_husb = Some(13), familles_refn = , familles_refn_type = , familles_wife = Some(12), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 3)
                          |(familles_resn = None, familles_husb = Some(9), familles_refn = , familles_refn_type = , familles_wife = Some(5), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 4)""".stripMargin,
    "genea_place" -> """(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(Haworth), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 1, place_ville = Some(Yorks.))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(Thornton), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 2, place_ville = Some(Nr. Bradford))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(Clough House), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 3, place_ville = Some(High Town))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(County Down), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 4, place_ville = Some(Ireland))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 5, place_ville = Some(Scarborough))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(Penzance), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 6, place_ville = Some(Cornwall))
                       |(place_longitude = None, place_pays = None, place_cp = None, place_region = None, place_lieudit = Some(), place_insee = None, place_departement = None, place_latitude = None, base = Some(1), place_id = 7, place_ville = Some(Howarth))""".stripMargin,
    "genea_infos" -> """(ged_corp = , nom = test, descriptif = , subm = None, entetes = , medias = None, id = 1)"""
  )

  "gedcom2sql" must {
    "work with bronte.ged" in {
      when(mockJourneyCacheRepository.get(eqTo(PlacesElementsSeparatorQuestion))(using any(), any())).thenReturn(
        Future.successful(Some(PlacesElementsSeparatorForm(",")))
      )
      when(mockJourneyCacheRepository.get(eqTo(PlacesElementsPaddingQuestion))(using any(), any())).thenReturn(
        Future.successful(Some(PlacesElementsPaddingForm("left")))
      )
      when(mockJourneyCacheRepository.get(eqTo(PlacesElementsQuestion))(using any(), any())).thenReturn(
        Future.successful(
          Some(
            PlacesElementsForm(List(PlaceSubdivisionMapping.Locality.toString, PlaceSubdivisionMapping.City.toString))
          )
        )
      )

      val parser: ResultSetParser[Option[Int]] = {
        int("insert_id").singleOpt
      }

      val rowParser: RowParser[Row] =
        RowParser { row =>
          Success(row)
        }

      val result = (for {
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
        _      <- sut.insertGedcomInDatabase("test/resources/gedcom/bronte.ged", 1)(using authenticatedRequest)
        result <- Future {
          db.withConnection { implicit conn =>
            val tables =
              SQL"""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
              """.as(SqlParser.str("table_name").*)

            tables.map { t =>
              val rows =
                SQL(s"SELECT * FROM `$t`")
                  .as(rowParser.*)

              val parsedRows = rows
                .map { row =>
                  row.asMap
                    .map {
                      case (col, _) if col.contains("timestamp") =>
                        s"${col.split("\\.").last} = xxxx-xx-xx xx:xx:xx"
                      case (col, value) =>
                        s"${col.split("\\.").last} = ${Option(value).getOrElse("NULL")}"
                    }
                    .mkString("(", ", ", ")")
                }
                .mkString("\n")

              t -> parsedRows
            }.toMap
          }
        }
      } yield result).futureValue

      result.foreach { (key, value) =>
        (expectedOutput.getOrElse(key, "") mustBe value).withClue(s" in table $key")
      }
    }
  }
}

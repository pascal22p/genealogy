package services.gedcom

import java.time.LocalDateTime

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

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

class GedcomImportServiceTortureSpec extends MariadbHelper {

  lazy val sut: GedcomImportService = app.injector.instanceOf[GedcomImportService]

  private val gedcomBasePath: String                                     = "test/resources/gedcom/"
  val gedcomPath: String                                                 = gedcomBasePath + "TGC551LF.ged"
  val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      FakeRequest(),
      Session("", SessionData(Some(UserData(0, "", "", false, false))), LocalDateTime.now),
      None
    )

  val expectedOutput: Map[String, String] = Map(
    "genea_individuals" -> """(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = ANSEL, indi_resn = None, indi_prenom = Charlie Accented, indi_id = 1, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = ANSEL, indi_resn = None, indi_prenom = Lucy Special, indi_id = 2, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Caregiver, indi_resn = None, indi_prenom = Teresa Mary, indi_id = 3, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Filelinks, indi_resn = None, indi_prenom = Extra URL, indi_id = 4, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Filelinks, indi_resn = None, indi_prenom = General Custom, indi_id = 5, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Filelinks, indi_resn = None, indi_prenom = Nonstandard Multimedia, indi_id = 6, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Filelinks, indi_resn = None, indi_prenom = Standard GEDCOM, indi_id = 7, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Jones, indi_resn = None, indi_prenom = Mary First, indi_id = 8, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Matriarch, indi_resn = None, indi_prenom = Torture GEDCOM, indi_id = 9, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = F, indi_nom = Smith, indi_resn = None, indi_prenom = Elizabeth Second, indi_id = 10, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = , indi_nom = Torture, indi_resn = Some(locked), indi_prenom = Chris Locked, indi_id = 11, indi_spfx = , indi_givn = )
                             |(indi_npfx = Prof., indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = Jr., indi_nick = Joe, base = 1, indi_sexe = M, indi_nom = Torture, indi_resn = None, indi_prenom = Joseph Tag, indi_id = 12, indi_spfx = Le, indi_givn = Joseph)
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = , indi_nom = Torture, indi_resn = None, indi_prenom = Pat Smith, indi_id = 13, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = , indi_nom = Torture, indi_resn = Some(privacy), indi_prenom = Sandy Privacy, indi_id = 14, indi_spfx = , indi_givn = )
                             |(indi_npfx = , indi_timestamp = xxxx-xx-xx xx:xx:xx, indi_nsfx = , indi_nick = , base = 1, indi_sexe = M, indi_nom = Torture, indi_resn = None, indi_prenom = William Joseph, indi_id = 15, indi_spfx = , indi_givn = )""".stripMargin,
    "rel_familles_indi" -> """(indi_id = 15, familles_id = 1, rela_type = , rela_stat = None)
                             |(indi_id = 1, familles_id = 2, rela_type = , rela_stat = None)
                             |(indi_id = 12, familles_id = 3, rela_type = , rela_stat = None)
                             |(indi_id = 4, familles_id = 4, rela_type = , rela_stat = None)
                             |(indi_id = 5, familles_id = 4, rela_type = , rela_stat = None)
                             |(indi_id = 6, familles_id = 4, rela_type = , rela_stat = None)
                             |(indi_id = 11, familles_id = 5, rela_type = , rela_stat = None)
                             |(indi_id = 14, familles_id = 5, rela_type = , rela_stat = None)
                             |(indi_id = 13, familles_id = 6, rela_type = , rela_stat = None)
                             |(indi_id = 12, familles_id = 7, rela_type = , rela_stat = None)""".stripMargin,
    "rel_indi_events" -> """(indi_id = 1, events_tag = BIRT, events_attestation = None, events_details_id = 1, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 1, events_tag = DEAT, events_attestation = None, events_details_id = 2, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 2, events_tag = BIRT, events_attestation = None, events_details_id = 3, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 2, events_tag = DEAT, events_attestation = None, events_details_id = 4, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 3, events_tag = BIRT, events_attestation = None, events_details_id = 5, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 4, events_tag = BIRT, events_attestation = None, events_details_id = 6, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 5, events_tag = BIRT, events_attestation = None, events_details_id = 7, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 5, events_tag = DEAT, events_attestation = None, events_details_id = 8, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 6, events_tag = BIRT, events_attestation = None, events_details_id = 9, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 6, events_tag = DEAT, events_attestation = None, events_details_id = 10, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 7, events_tag = BIRT, events_attestation = None, events_details_id = 11, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 8, events_tag = BIRT, events_attestation = None, events_details_id = 12, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 8, events_tag = DEAT, events_attestation = None, events_details_id = 13, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 9, events_tag = BIRT, events_attestation = None, events_details_id = 14, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 9, events_tag = DEAT, events_attestation = None, events_details_id = 15, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 10, events_tag = BIRT, events_attestation = None, events_details_id = 16, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 10, events_tag = DEAT, events_attestation = None, events_details_id = 17, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 11, events_tag = BIRT, events_attestation = None, events_details_id = 18, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BIRT, events_attestation = None, events_details_id = 19, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = DEAT, events_attestation = None, events_details_id = 20, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BAPM, events_attestation = None, events_details_id = 21, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CHR, events_attestation = None, events_details_id = 22, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CHR, events_attestation = None, events_details_id = 23, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BLES, events_attestation = None, events_details_id = 24, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BARM, events_attestation = None, events_details_id = 25, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BASM, events_attestation = None, events_details_id = 26, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = ADOP, events_attestation = None, events_details_id = 27, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CHRA, events_attestation = None, events_details_id = 28, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CONF, events_attestation = None, events_details_id = 29, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = FCOM, events_attestation = None, events_details_id = 30, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = ORDN, events_attestation = None, events_details_id = 31, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = GRAD, events_attestation = None, events_details_id = 32, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = EMIG, events_attestation = None, events_details_id = 33, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = IMMI, events_attestation = None, events_details_id = 34, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = NATU, events_attestation = None, events_details_id = 35, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CENS, events_attestation = None, events_details_id = 36, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = RETI, events_attestation = None, events_details_id = 37, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = PROB, events_attestation = None, events_details_id = 38, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = BURI, events_attestation = None, events_details_id = 39, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = WILL, events_attestation = None, events_details_id = 40, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CREM, events_attestation = None, events_details_id = 41, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = EVEN, events_attestation = None, events_details_id = 42, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = RESI, events_attestation = None, events_details_id = 43, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = OCCU, events_attestation = None, events_details_id = 44, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = OCCU, events_attestation = None, events_details_id = 45, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = EDUC, events_attestation = None, events_details_id = 46, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = DSCR, events_attestation = None, events_details_id = 47, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = RELI, events_attestation = None, events_details_id = 48, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = SSN, events_attestation = None, events_details_id = 49, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = IDNO, events_attestation = None, events_details_id = 50, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = PROP, events_attestation = None, events_details_id = 51, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = CAST, events_attestation = None, events_details_id = 52, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = NCHI, events_attestation = None, events_details_id = 53, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = NMR, events_attestation = None, events_details_id = 54, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = TITL, events_attestation = None, events_details_id = 55, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 12, events_tag = NATI, events_attestation = None, events_details_id = 56, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 13, events_tag = BIRT, events_attestation = None, events_details_id = 57, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 14, events_tag = BIRT, events_attestation = None, events_details_id = 58, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 15, events_tag = BIRT, events_attestation = None, events_details_id = 59, timestamp = xxxx-xx-xx xx:xx:xx)
                           |(indi_id = 15, events_tag = DEAT, events_attestation = None, events_details_id = 60, timestamp = xxxx-xx-xx xx:xx:xx)""".stripMargin,
    "genea_events_details" -> """(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 JUN 1900, events_details_descriptor = , events_details_id = 1, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 5 JUL 1974, events_details_descriptor = , events_details_id = 2, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 12 AUG 1905, events_details_descriptor = , events_details_id = 3, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1990, events_details_descriptor = , events_details_id = 4, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 6 JUN 1944, events_details_descriptor = , events_details_id = 5, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1875, events_details_descriptor = , events_details_id = 6, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1872, events_details_descriptor = , events_details_id = 7, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 7 DEC 1941, events_details_descriptor = , events_details_id = 8, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1870, events_details_descriptor = , events_details_id = 9, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = , events_details_descriptor = , events_details_id = 10, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1835, events_details_descriptor = , events_details_id = 11, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = BEF 1970, events_details_descriptor = , events_details_id = 12, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = AFT 2000, events_details_descriptor = , events_details_id = 13, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 12 FEB 1840, events_details_descriptor = , events_details_id = 14, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 JUN 1915, events_details_descriptor = , events_details_id = 15, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = BET MAY 1979 AND AUG 1979, events_details_descriptor = , events_details_id = 16, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = FROM APR 2000 TO 5 MAR 2001, events_details_descriptor = , events_details_id = 17, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(5), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = MAR 1999, events_details_descriptor = , events_details_id = 18, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(10), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1965, events_details_descriptor = , events_details_id = 19, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(6), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = ABT 15 JAN 2001, events_details_descriptor = , events_details_id = 20, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = ABT 31 DEC 1997, events_details_descriptor = , events_details_id = 21, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = CAL 31 DEC 1997, events_details_descriptor = , events_details_id = 22, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = EST 30 DEC 1997, events_details_descriptor = , events_details_id = 23, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = BEF 31 DEC 1997, events_details_descriptor = , events_details_id = 24, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = AFT 31 DEC 1997, events_details_descriptor = , events_details_id = 25, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = FROM 31 DEC 1997, events_details_descriptor = , events_details_id = 26, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = TO 31 DEC 1997, events_details_descriptor = , events_details_id = 27, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = BET 31 DEC 1997 AND 1 FEB 1998, events_details_descriptor = , events_details_id = 28, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = FROM 31 DEC 1997 TO 2 JAN 1998, events_details_descriptor = , events_details_id = 29, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = INT 31 DEC 1997 (a test), events_details_descriptor = , events_details_id = 30, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = (No idea of the date), events_details_descriptor = , events_details_id = 31, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 32, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1997, events_details_descriptor = , events_details_id = 33, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = DEC 1997, events_details_descriptor = , events_details_id = 34, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 5 AUG 1100 B.C., events_details_descriptor = , events_details_id = 35, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 2 TVT 5758, events_details_descriptor = , events_details_id = 36, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 11 NIVO 0006, events_details_descriptor = , events_details_id = 37, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = FROM 25 SVN 5757 TO 26 IYR 5757, events_details_descriptor = , events_details_id = 38, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 5 VEND 0010, events_details_descriptor = , events_details_id = 39, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = INT 2 TVT 5758 (interpreted Hebrew date), events_details_descriptor = , events_details_id = 40, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = , events_details_descriptor = , events_details_id = 41, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 5 MAY 0005, events_details_descriptor = , events_details_id = 42, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 43, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 44, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1998, events_details_descriptor = , events_details_id = 45, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 46, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 47, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 48, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 49, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 50, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 51, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 52, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 53, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 54, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 55, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(2), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 31 DEC 1997, events_details_descriptor = , events_details_id = 56, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(8), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 1 JAN 2001, events_details_descriptor = , events_details_id = 57, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = Some(3), jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = 15 FEB 2000, events_details_descriptor = , events_details_id = 58, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = ABT 1930, events_details_descriptor = , events_details_id = 59, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )
                                |(place_id = None, jd_precision = None, events_details_cause = , events_details_famc = None, events_details_gedcom_date = INT 1995 (from estimated age), events_details_descriptor = , events_details_id = 60, jd_calendar = None, events_details_adop = None, base = 1, events_details_timestamp = xxxx-xx-xx xx:xx:xx, addr_id = None, jd_count = None, events_details_age = )""".stripMargin,
    "genea_familles" -> """(familles_resn = None, familles_husb = Some(1), familles_refn = , familles_refn_type = , familles_wife = Some(2), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 1)
                          |(familles_resn = None, familles_husb = None, familles_refn = , familles_refn_type = , familles_wife = Some(6), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 2)
                          |(familles_resn = None, familles_husb = None, familles_refn = , familles_refn_type = , familles_wife = Some(3), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 3)
                          |(familles_resn = None, familles_husb = Some(7), familles_refn = , familles_refn_type = , familles_wife = Some(9), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 4)
                          |(familles_resn = None, familles_husb = Some(12), familles_refn = , familles_refn_type = , familles_wife = Some(8), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 5)
                          |(familles_resn = None, familles_husb = Some(12), familles_refn = , familles_refn_type = , familles_wife = Some(10), base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 6)
                          |(familles_resn = None, familles_husb = Some(15), familles_refn = , familles_refn_type = , familles_wife = None, base = Some(1), familles_timestamp = xxxx-xx-xx xx:xx:xx, familles_id = 7)""".stripMargin,
    "genea_infos" -> """(ged_corp = , nom = test, descriptif = , subm = None, entetes = , medias = None, id = 1)"""
  )

  "gedcom2sql" must {
    "work with TGC551LF.ged" in {
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
        _      <- sut.insertGedcomInDatabase(gedcomPath, 1, "jobId")(using authenticatedRequest)
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
        expectedOutput.getOrElse(key, "").lines().toList.asScala.zip(value.lines().toList.asScala).zipWithIndex.map {
          case ((expectedLine, line), idx) =>
            (expectedLine mustBe line).withClue(
              s" in table $key, line ${idx + 1}\nexpected: `$expectedLine`\nobtained: `$line`"
            )
        }
      }
    }
  }
}

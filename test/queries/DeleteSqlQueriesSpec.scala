package queries

import models.*
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.Logging
import testUtils.MariadbHelper

class DeleteSqlQueriesSpec extends MariadbHelper with Logging {
  lazy val databaseExecutionContext: DatabaseExecutionContext = app.injector.instanceOf[DatabaseExecutionContext]
  lazy val getSqlQueries: GetSqlQueries                       = new GetSqlQueries(db, databaseExecutionContext)
  lazy val sut: DeleteSqlQueries                              = new DeleteSqlQueries(db, databaseExecutionContext)

  implicit val request: Request[?] = FakeRequest()
    .withHeaders("X-Request-Id" -> "requestID")
    .addAttr(Attrs.RequestId, "requestID")
    .addAttr(Attrs.SessionId, "sessionID")

  def sqlGenealogyDatabase(id: Int): String =
    s"""INSERT INTO `genea_infos` (`id`, `nom`, `descriptif`, `entetes`, `ged_corp`, `subm`) VALUES
       |($id, 'Name', 'Description', '', '', NULL);
       |""".stripMargin

  def sqlSourRecord(id: Int, baseId: Int = 1): String =
    s"""INSERT INTO `genea_sour_records` (`sour_records_id`, `sour_records_auth`, `sour_records_title`, `sour_records_abbr`, `sour_records_publ`, `sour_records_agnc`, `sour_records_rin`, `repo_id`, `repo_caln`, `repo_medi`, `base`) VALUES
       |($id, 'Author', 'Title', 'Abbr', 'Publ', 'Agnc', 'RIN', NULL, 'CALN', 'Book', $baseId);
       |""".stripMargin

  def sqlSourCitation(id: Int, recordId: Option[Int], baseId: Int = 1): String =
    s"""INSERT INTO `genea_sour_citations` (`sour_citations_id`, `sour_records_id`, `sour_citations_page`, `sour_citations_even`, `sour_citations_even_role`, `sour_citations_data_dates`, `sour_citations_data_text`, `sour_citations_quay`, `sour_citations_subm`, `base`) VALUES
       |($id, ${recordId.fold("NULL")(_.toString)}, 'p. 1', '', '', '', 'Text', 1, '', $baseId);
       |""".stripMargin

  def sqlNote(id: Int, baseId: Int = 1): String =
    s"""INSERT INTO `genea_notes` (`notes_id`, `notes_text`, `base`) VALUES
       |($id, 'Some note text', $baseId);
       |""".stripMargin

  def sqlRelSourRecordNote(sourRecordId: Int, noteId: Int): String =
    s"""INSERT INTO `rel_sour_records_notes` (`notes_id`, `sour_records_id`) VALUES
       |($noteId, $sourRecordId);
       |""".stripMargin

  "deleteSourRecord" must {
    "delete sour record and disassociate related citations and notes" in {
      val sourRecordId = 1
      val citationId   = 10
      val noteId       = 20

      val result = (for {
        _             <- executeSql(sqlGenealogyDatabase(1))
        _             <- executeSql(sqlSourRecord(sourRecordId, 1))
        _             <- executeSql(sqlSourCitation(citationId, Some(sourRecordId), 1))
        _             <- executeSql(sqlNote(noteId, 1))
        _             <- executeSql(sqlRelSourRecordNote(sourRecordId, noteId))
        _             <- sut.deleteSourRecord(1, sourRecordId)
        deletedRecord <- getSqlQueries.getSourRecord(1, sourRecordId).value
        citations     <- getSqlQueries.getSourCitationsFromRecord(sourRecordId)
      } yield (deletedRecord, citations)).futureValue

      result._1 mustBe None
      result._2 mustBe empty
    }
  }
}

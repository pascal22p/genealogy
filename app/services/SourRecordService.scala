package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import models.SourRecord
import queries.GetSqlQueries

@Singleton
class SourRecordService @Inject() (mariadbQueries: GetSqlQueries)() {
  def getSourRecord(sourRecordId: Int): Future[Option[SourRecord]] = {
    mariadbQueries.getSourRecord(sourRecordId)
  }

}

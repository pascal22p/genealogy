package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import cats.data.OptionT
import models.SourRecord
import queries.GetSqlQueries

@Singleton
class SourRecordService @Inject() (mariadbQueries: GetSqlQueries)() {
  def getSourRecord(sourRecordId: Int): OptionT[Future, SourRecord] = {
    mariadbQueries.getSourRecord(sourRecordId)
  }

}

package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import cats.data.OptionT
import models.SourRecord
import queries.GetSqlQueries
import io.opentelemetry.instrumentation.annotations.WithSpan

@Singleton
class SourRecordService @Inject() (mariadbQueries: GetSqlQueries)() {
  @WithSpan
  def getSourRecord(sourRecordId: Int): OptionT[Future, SourRecord] = {
    mariadbQueries.getSourRecord(sourRecordId)
  }

}

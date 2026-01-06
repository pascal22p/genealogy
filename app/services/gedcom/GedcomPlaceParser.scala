package services.gedcom

import javax.inject.Inject

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext

import anorm.BatchSql
import anorm.NamedParameter
import models.gedcom.PlaceSubdivisionMapping
import models.gedcom.PlaceSubdivisionMapping.*
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import repositories.JourneyCacheRepository

class GedcomPlaceParser @Inject() (
    gedcomHashIdTable: GedcomHashIdTable,
    journeyCacheRepository: JourneyCacheRepository
)(
    implicit ec: ExecutionContext
) {

  @SuppressWarnings(Array("org.wartremover.warts.EnumValueOf"))
  def readPlaceBlocks(
      places: List[String],
      jobId: String
  )(implicit request: AuthenticatedRequest[?]): Map[Int, Map[PlaceSubdivisionMapping, String]] = {
    Await.result(
      for {
        maybeSeparator     <- journeyCacheRepository.get(PlacesElementsSeparatorQuestion)
        maybePadding       <- journeyCacheRepository.get(PlacesElementsPaddingQuestion)
        maybeFieldsMapping <- journeyCacheRepository.get(PlacesElementsQuestion)
      } yield {
        (maybeSeparator, maybePadding, maybeFieldsMapping) match {
          case (Some(separator), Some(paddingOrder), Some(fieldsMapping)) =>
            val splitLines =
              places.distinct.map(line => line -> line.split(separator.separator).map(_.trim).toList).toMap
            val maxSize          = splitLines.map(_._2.size).max
            val paddedSplitLines = splitLines.map {
              case (line, parts) =>
                val padSize = maxSize - parts.size
                if (paddingOrder.padding == "left") {
                  line -> (List.fill(padSize)("") ++ parts)
                } else {
                  line -> (parts ++ List.fill(padSize)(""))
                }
            }
            val fieldsToIndices = fieldsMapping.hierarchy.zipWithIndex.groupMap(_._1)(_._2)
            paddedSplitLines.map {
              case (line, parts) =>
                val id = gedcomHashIdTable.getPlaceIdFromString(jobId, line)
                (
                  id,
                  fieldsToIndices.map {
                    case (fieldType, indices) =>
                      PlaceSubdivisionMapping
                        .valueOf(fieldType) -> indices
                        .map(parts)
                        .filter(_.trim.nonEmpty)
                        .mkString(separator.separator + " ")
                  }
                )
            }

          case _ => throw new RuntimeException("Some elements missing from cache")
        }
      },
      2.minutes
    )
  }

  def placeBlocks2Sql(places: Map[Int, Map[PlaceSubdivisionMapping, String]], base: Int): Iterator[BatchSql] = {
    val sqlStatement =
      s"""INSERT INTO `genea_place` (`place_id`, `place_lieudit`, `place_ville`, `place_cp`, `place_insee`, `place_departement`, `place_region`, `place_pays`, `place_longitude`, `place_latitude`, `base`)
         |VALUES ({place_id} + @startPlace, {locality}, {town}, {postcode}, {insee}, {subdivision}, {division}, {country}, {longitude}, {latitude}, {base});""".stripMargin

    val parametersGroups = places.iterator.map {
      case (id, data) =>
        Seq[NamedParameter](
          "place_id"    -> id,
          "base"        -> base,
          "locality"    -> data.get(Locality),
          "town"        -> data.get(City),
          "postcode"    -> data.get(Postcode),
          "insee"       -> data.get(Insee).flatMap(_.toIntOption),
          "subdivision" -> data.get(Subdivision),
          "division"    -> data.get(Division),
          "country"     -> data.get(Country),
          "longitude"   -> data.get(Longitude).flatMap(_.toFloatOption),
          "latitude"    -> data.get(Latitude).flatMap(_.toFloatOption)
        )
    }

    parametersGroups.grouped(100).map { parameters =>
      BatchSql(sqlStatement, parameters.head, parameters.tail*)
    }
  }
}

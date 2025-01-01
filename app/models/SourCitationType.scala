package models
import play.api.data.format.Formatter
import play.api.data.FormError
import play.api.mvc.PathBindable

object SourCitationType {

  def fromString(s: String): SourCitationType = {
    s match {
      case s if s == EventSourCitation.toString      => EventSourCitation
      case s if s == IndividualSourCitation.toString => IndividualSourCitation
      case s if s == FamilySourCitation.toString     => FamilySourCitation
      case _                                         => UnknownSourCitation
    }
  }

  val formatter: Formatter[SourCitationType] = new Formatter[SourCitationType] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], SourCitationType] = {
      data
        .get(key)
        .map(SourCitationType.fromString)
        .toRight(Seq(FormError(key, "error.required", Nil)))
    }

    override def unbind(key: String, value: SourCitationType): Map[String, String] = {
      Map(key -> value.toString)
    }
  }

  implicit val pathBindable: PathBindable[SourCitationType] = new PathBindable[SourCitationType] {
    override def bind(key: String, value: String): Either[String, SourCitationType] = {
      try {
        Right[String, SourCitationType](SourCitationType.fromString(value))
      } catch {
        case _: Exception => Left[String, SourCitationType]("Unable to bind a SourCitationType")
      }
    }

    override def unbind(key: String, value: SourCitationType): String = {
      value.toString
    }
  }

  sealed trait SourCitationType {
    def toString: String
  }

  case object EventSourCitation extends SourCitationType {
    override def toString = "event-source"
  }

  case object IndividualSourCitation extends SourCitationType {
    override def toString = "individual-source"
  }

  case object FamilySourCitation extends SourCitationType {
    override def toString = "family-source"
  }

  case object UnknownSourCitation extends SourCitationType {
    override def toString = "unknown-source"
  }
}

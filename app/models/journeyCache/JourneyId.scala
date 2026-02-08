package models.journeyCache

// A unique identifier for different user journeys
enum JourneyId {
  case ImportGedcom
  case AddIndividualToFamily
}

object JourneyId {
  given CanEqual[JourneyId, JourneyId] = CanEqual.derived
}

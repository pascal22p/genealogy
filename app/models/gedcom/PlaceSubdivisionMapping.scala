package models.gedcom

// mapping used to map subdivisions of a place to their corresponding field in the SQL database
enum PlaceSubdivisionMapping {
  case Locality, City, Postcode, Insee, Subdivision, Division, Country, Longitude, Latitude
  // Insee is the French national institute of statistics and economic studies code for places
  // subdivision and division are generic terms for administrative areas like states, provinces, counties, etc. subdivisions is smaller than divisions
}

package models

final case class GedcomObjectUsage(
    gedcomObjectId: Int,
    ownerType: GedcomObjectType,
    ownerId: Int,
    personIds: List[Int] = Nil
)

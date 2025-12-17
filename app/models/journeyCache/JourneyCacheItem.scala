package models.journeyCache

import play.api.mvc.Call

sealed trait JourneyCacheItem extends Product {
  def name: String                  = productPrefix
  def requirement: ItemRequirements = ItemRequirements.Always()
  def page: Call
}

case object GedcomPath extends JourneyCacheItem {
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.showGedcomList
}
case object NewDatabaseQuestion extends JourneyCacheItem {
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion
}
case object NewDatabaseName extends JourneyCacheItem {
  override def requirement: ItemRequirements =
    ItemRequirements.IfStringIs(NewDatabaseQuestion, "true")
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.addNewDatabase
}
case object NewDatabaseDescription extends JourneyCacheItem {
  override def requirement: ItemRequirements =
    ItemRequirements.IfStringIs(NewDatabaseQuestion, "true")
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.addNewDatabase
}

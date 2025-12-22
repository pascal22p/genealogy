package models.gedcom

final case class GedcomObject(nodes: List[GedcomNode]) {
  def getHusb(id: String): Option[String] = {
    val husb = nodes.find(_.xref.contains(id)).flatMap(_.children.find(_.name == "HUSB").flatMap(_.xref))
    husb.flatMap { idHusb =>
      nodes.find(_.xref.contains(idHusb)).flatMap { node =>
        node.children.find(_.name == "NAME").flatMap(_.content)
      }
    }
  }

  def getWife(id: String): Option[String] = {
    val wife = nodes.find(_.xref.contains(id)).flatMap(_.children.find(_.name == "WIFE").flatMap(_.xref))
    wife.flatMap { idWife =>
      nodes.find(_.xref.contains(idWife)).flatMap { node =>
        node.children.find(_.name == "NAME").flatMap(_.content)
      }
    }
  }

  def getIndividualsSummary: List[String] = {
    nodes.filter(_.name == "INDI").map { indiNode =>
      val name = indiNode.children.find(_.name == "NAME").flatMap(_.content)
      s"""${indiNode.lineNumber} / ${indiNode.xref.getOrElse("Unknown ID")}: ${name.getOrElse("")}"""
    }
  }

  def getFamiliesSummary: List[String] = {
    nodes.filter(_.name == "FAM").map { familyNode =>
      val husb = familyNode.xref.flatMap(getHusb).fold("")(husb => s"Husband: $husb,")
      val wife = familyNode.xref.flatMap(getWife).fold("")(wife => s"Wife: $wife")
      s"""${familyNode.lineNumber} / ${familyNode.xref.getOrElse("Unknown ID")}. $husb $wife"""
    }
  }

}

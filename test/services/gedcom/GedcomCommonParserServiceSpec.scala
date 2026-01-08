package services.gedcom

import models.gedcom.GedcomNode
import testUtils.BaseSpec

class GedcomCommonParserServiceSpec extends BaseSpec {

  val sut = new GedcomCommonParser()

  "getBlocks" must {
    "works" in {
      val input = """
                    |0 HEAD
                    |0 @I1@ INDI
                    |1 NAME First1 /Name1/
                    |0 @I2@ INDI
                    |1 NAME First2 /Name2/
                    |  2 PLAC Somewhere
                    |   3 BIRT deeper
                    |0 @I3@ INDI
                    |1 NAME First3 /Name3/
                    |""".stripMargin.linesIterator

      val expected = Seq(
        (1, Vector("0 HEAD")),
        (2, Vector("0 @I1@ INDI", "1 NAME First1 /Name1/")),
        (4, Vector("0 @I2@ INDI", "1 NAME First2 /Name2/", "2 PLAC Somewhere", "3 BIRT deeper")),
        (8, Vector("0 @I3@ INDI", "1 NAME First3 /Name3/"))
      )

      val result = sut.getBlocks(input).toSeq
      result mustBe expected
    }

    "parse a single line" in {
      val input = "1 NAME First2 /Name2/".linesIterator

      val expected = Seq((0, Vector("1 NAME First2 /Name2/")))

      val result = sut.getBlocks(input, 1).toSeq
      result mustBe expected
    }

    "return an error if level is incorrect" in {
      val input = "1 NAME First2 /Name2/".linesIterator

      val exception = intercept[RuntimeException] {
        sut.getBlocks(input, 2).toSeq
      }
      exception.getMessage mustBe "Expected block start level 2 at line 0, found: 1 NAME First2 /Name2/"
    }
  }

  "getListNodes" must {
    "works" in {
      val input =
        """
          |0 HEAD
          |0 @I1@ INDI
          |1 NAME First1 /Name1/
          |0 @I2@ INDI
          |1 NAME First2 /Name2/
          |0 @I3@ INDI
          |1 NAME First3 /Name3/
          |""".stripMargin.linesIterator

      val expected: Seq[GedcomNode] = Seq(
        GedcomNode(
          name = "HEAD",
          line = "0 HEAD",
          lineNumber = 1,
          level = 0,
          xref = None,
          content = None,
          children = List()
        ),
        GedcomNode(
          name = "INDI",
          line = "0 @I1@ INDI",
          lineNumber = 2,
          level = 0,
          xref = Some("I1"),
          content = None,
          children = List(
            GedcomNode(
              name = "NAME",
              line = "1 NAME First1 /Name1/",
              lineNumber = 3,
              level = 1,
              xref = None,
              content = Some("First1 /Name1/"),
              children = List()
            )
          )
        ),
        GedcomNode(
          name = "INDI",
          line = "0 @I2@ INDI",
          lineNumber = 4,
          level = 0,
          xref = Some("I2"),
          content = None,
          children = List(
            GedcomNode(
              name = "NAME",
              line = "1 NAME First2 /Name2/",
              lineNumber = 5,
              level = 1,
              xref = None,
              content = Some("First2 /Name2/"),
              children = List()
            )
          )
        ),
        GedcomNode(
          name = "INDI",
          line = "0 @I3@ INDI",
          lineNumber = 6,
          level = 0,
          xref = Some("I3"),
          content = None,
          children = List(
            GedcomNode(
              name = "NAME",
              line = "1 NAME First3 /Name3/",
              lineNumber = 7,
              level = 1,
              xref = None,
              content = Some("First3 /Name3/"),
              children = List()
            )
          )
        )
      )

      val result = sut.getListNodes(input).toSeq
      result mustBe expected
    }

    "works with a single line and no content" in {
      val input =
        "1 BIRT".linesIterator

      val expected: Seq[GedcomNode] = Seq(GedcomNode("BIRT", "1 BIRT", 0, 1, None, None, List()))

      val result = sut.getListNodes(input, 1).toSeq
      result mustBe expected
    }
  }

}

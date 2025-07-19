package services

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

import scala.sys.process.*
import scala.sys.process.ProcessLogger

import config.AppConfig
import models.AuthenticatedRequest
import models.Family
import models.Person
import play.api.i18n.Messages

@Singleton
class GraphVizDotService @Inject() () {

  def treeToDot(
      tree: Tree,
      origin: Int
  )(implicit messages: Messages, request: AuthenticatedRequest[?], appConfig: AppConfig): String = {

    def individualNode(person: (Int, Person)): String = {
      val attributes = if (person._2.details.id == origin) {
        "style=filled fillcolor=palegoldenrod"
      } else {
        ""
      }
      s"I${person._1} [label=\"${person._2.shortName}\\n${person._2.events.birthAndDeathDate(true)}\" $attributes];"
    }

    def familyNode(family: (Int, Family)): String =
      s"F${family._1} [label=\"${family._2.events.weddingDate(true)}\" shape=oval];"

    val dotHeader =
      s"""
         |digraph family {
         |    ranksep="0.3";
         |    node [shape = record, fontname="Arial",fontsize="16"];
         |""".stripMargin

    val individualsInGroups      = tree.groups.values.flatten.toList
    val individualsWithoutGroups = tree.individuals.filterNot(x => individualsInGroups.contains(x._1))

    val dotGroups = tree.groups
      .map { group =>
        s"subgraph cluster_${group._1} {\n" +
          "peripheries=0\n" +
          group._2
            .map { indiId =>
              tree.individuals
                .find(_._1 == indiId)
                .map(individualNode)
                .getOrElse("")
            }
            .mkString("", "\n", "") +
          "}"
      }
      .mkString("", "\n", "\n")

    val dotNoGroups = "node[group=\"\"];\n" +
      individualsWithoutGroups
        .map(individualNode)
        .mkString("", "\n", "\n")

    val dotFamilies = tree.families
      .map(familyNode)
      .mkString("", "\n", "\n")

    val dotLinks = tree.links
      .map { link =>
        s"${link._1} -> ${link._2};"
      }
      .mkString("", "\n", "\n")

    val dotFooter = "}\n"

    Seq(
      dotHeader,
      dotGroups,
      dotNoGroups,
      dotFamilies,
      dotLinks,
      dotFooter
    ).mkString("\n")
  }

  def generateImageTree(dotString: String, fileType: String): Array[Byte] = {
    val dotProcess = Process(s"dot -T$fileType")
    val stdout     = new ByteArrayOutputStream()
    val stderr     = new StringBuilder

    val logger = ProcessLogger(
      _ => (),
      err => stderr.append(err + "\n")
    )

    // Run dot with DOT input passed via stdin
    val exitCode = dotProcess #< new ByteArrayInputStream(dotString.getBytes("UTF-8")) #> stdout ! logger

    if (exitCode == 0) stdout.toByteArray
    else {
      throw new RuntimeException("dot command failed with exit code: " + exitCode + "\n" + stderr.toString())
    }
  }
}

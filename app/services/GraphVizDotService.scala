package services

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

import scala.sys.process.*
import scala.sys.process.ProcessLogger

import config.AppConfig
import models.AuthenticatedRequest
import models.LoggingWithRequest
import models.Tree
import play.api.i18n.Messages
import views.txt.dotTemplates.DotMainView
import play.api.Logging

@Singleton
class GraphVizDotService @Inject() (
    dotMainView: DotMainView
) extends Logging
    with LoggingWithRequest {

  def treeToDot(
      tree: Tree,
      origin: Int
  )(implicit messages: Messages, request: AuthenticatedRequest[?], appConfig: AppConfig): String = {
    dotMainView(tree, origin).body
  }

  def generateImageTree(dotString: String, fileType: String): Array[Byte] = {
    val dotProcess = Process(s"dot -T$fileType")
    val stdout     = new ByteArrayOutputStream()
    val stderr     = new StringBuilder

    val procLogger = ProcessLogger(
      _ => (),
      err => stderr.append(err + "\n")
    )

    // Run dot with DOT input passed via stdin
    val exitCode = dotProcess #< new ByteArrayInputStream(dotString.getBytes("UTF-8")) #> stdout ! procLogger

    if (exitCode == 0) stdout.toByteArray
    else {
      throw new RuntimeException("dot command failed with exit code: " + exitCode + "\n" + stderr.toString())
    }
  }
}

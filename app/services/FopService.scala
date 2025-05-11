package services

import java.io.ByteArrayOutputStream
import java.io.StringReader
import javax.inject.Inject
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.TransformerFactory

import scala.concurrent.ExecutionContext

import models.AuthenticatedRequest
import models.Person
import org.apache.fop.apps.FopFactory
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.Messages
import views.xml.pdfTemplates.PdfCompactTree

class FopService @Inject() (compactTree: PdfCompactTree)(implicit val ec: ExecutionContext) {

  def xmlTopdf(
      sosaList: Map[Int, Person]
  )(implicit request: AuthenticatedRequest[?], messages: Messages): Array[Byte] = {
    val title = sosaList.get(1).fold("") { person =>
      s"Ascendance of ${person.name}"
    }

    // Initialize FOP
    val fopFactory = FopFactory.newInstance(new java.io.File(".").toURI)
    val baos       = new ByteArrayOutputStream()
    val fop        = fopFactory.newFop(MimeConstants.MIME_PDF, baos)

    // Transform XSL-FO to PDF
    val transformer = TransformerFactory.newInstance().newTransformer()
    val src         = new StreamSource(new StringReader(compactTree(title, sosaList).body))
    val res         = new SAXResult(fop.getDefaultHandler)
    transformer.transform(src, res)

    // Get the PDF bytes
    val pdfBytes = baos.toByteArray
    baos.close()
    pdfBytes
  }
}

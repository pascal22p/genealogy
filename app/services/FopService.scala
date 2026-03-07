package services

import java.io.ByteArrayOutputStream
import java.io.StringReader
import javax.inject.Inject
import javax.xml.parsers.SAXParserFactory
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
import org.xml.sax.InputSource

class FopService @Inject() (compactTree: PdfCompactTree)(implicit val ec: ExecutionContext) {

  def xmlTopdf(
      sosaList: Map[Int, Person]
  )(implicit request: AuthenticatedRequest[?], messages: Messages): Array[Byte] = {
    val title = sosaList.get(1).fold("") { person =>
      s"Ascendance of ${person.name}"
    }

    // Initialize FOP
    val xmlContent = compactTree(title, sosaList).body
    val fopFactory = FopFactory.newInstance(new java.io.File(".").toURI)
    val baos       = new ByteArrayOutputStream()
    val fop        = fopFactory.newFop(MimeConstants.MIME_PDF, baos)

    // Transform XSL-FO to PDF
    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)

    val parser    = factory.newSAXParser()
    val xmlReader = parser.getXMLReader

    xmlReader.setContentHandler(fop.getDefaultHandler)
    xmlReader.parse(new InputSource(new StringReader(xmlContent)))

    // Get the PDF bytes
    val pdfBytes = baos.toByteArray
    baos.close()
    pdfBytes
  }
}

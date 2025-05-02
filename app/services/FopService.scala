package services

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import javax.inject.Inject
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.TransformerFactory

import scala.concurrent.ExecutionContext

import org.apache.fop.apps.FopFactory
import org.apache.xmlgraphics.util.MimeConstants
import views.xml.xml.CompactTree

class FopService @Inject() (compactTree: CompactTree)(implicit val ec: ExecutionContext) {

  def xmlToPdf() = {
    val foContent = compactTree.render().toString

    val fopFactory  = FopFactory.newInstance(new File(".").toURI)
    val out         = new FileOutputStream(new File("output.pdf"))
    val fop         = fopFactory.newFop(MimeConstants.MIME_PDF, out)
    val transformer = TransformerFactory.newInstance().newTransformer()
    val src         = new StreamSource(new java.io.StringReader(foContent))
    val res         = new SAXResult(fop.getDefaultHandler)
    transformer.transform(src, res)
    out.close()
  }

  def xmlTopdf2() = {
    // Render the Twirl template to get XSL-FO XML
    val foContent = compactTree.render().toString

    // Initialize FOP
    val fopFactory = FopFactory.newInstance(new java.io.File(".").toURI)
    val baos       = new ByteArrayOutputStream()
    val fop        = fopFactory.newFop(MimeConstants.MIME_PDF, baos)

    // Transform XSL-FO to PDF
    val transformer = TransformerFactory.newInstance().newTransformer()
    val src         = new StreamSource(new StringReader(foContent))
    val res         = new SAXResult(fop.getDefaultHandler)
    transformer.transform(src, res)

    // Get the PDF bytes
    val pdfBytes = baos.toByteArray
    baos.close()
    pdfBytes
  }
}

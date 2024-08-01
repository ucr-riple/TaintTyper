package xmlStreamTest;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RUntainted;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

// Test basic subtyping relationships for the XXX Tainting Checker.
class XMLStreamTest {
  void test(@RUntainted InputStream in)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document;
    document = documentBuilder.parse(in);
  }

  public void testCaller(HttpServletRequest req)
      throws IOException, ParserConfigurationException, SAXException {
    // :: error: (argument)
    test(req.getInputStream());
  }
}

package tests.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Helper class for serialization tests. */
public abstract class SerializationTestHelper extends CheckerFrameworkPerDirectoryTest {

  public SerializationTestHelper(
      List<File> testFiles,
      Class<? extends AbstractProcessor> checker,
      String testDir,
      String... checkerOptions) {
    super(testFiles, checker, testDir, Collections.emptyList(), checkerOptions);
  }

  @Override
  public void run() {
    final String root = "tmp";
    File outDir = null;
    try {
      // make temp dir for serialization output
      outDir = Files.createTempDirectory(root).toFile();
      Path configPath = makeConfig(outDir);
      // update checker options
      checkerOptions.add("-A" + "serializationConfigPath=" + configPath);
      // run checker
      super.run();
    } catch (AssertionError error) {
      // Due to error in assertion.
      deleteTempDir(outDir);
      throw error;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    deleteTempDir(outDir);
  }

  /**
   * Deletes the given file or directory.
   *
   * @param file the file or directory to delete.
   */
  private static void deleteTempDir(File file) {
    if (file != null) {
      try {
        if (file.isDirectory()) {
          FileUtils.deleteDirectory(file);
        } else {
          FileUtils.delete(file);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Creates a config file for serialization.
   *
   * @param outDir the directory where the output files will be written.
   * @return the path to the created config file.
   */
  private static Path makeConfig(File outDir) {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    Path configPath = outDir.toPath().resolve("checker.xml");
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      // Root
      Element rootElement = doc.createElement("serialization");
      doc.appendChild(rootElement);

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(outDir.toPath().resolve("0").toAbsolutePath().toString());
      rootElement.appendChild(outputDir);

      // Writings
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(configPath.toFile());
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      throw new RuntimeException("Error happened in writing config.", e);
    }
    return configPath;
  }
}

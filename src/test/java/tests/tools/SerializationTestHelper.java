package tests.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.json.JSONArray;
import org.json.JSONObject;
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
    final String root = "ucrtaint-tmp";
    File outDir = null;
    try {
      // make temp dir for serialization output
      outDir = Files.createTempDirectory(root).toFile();
      Path configPath = makeConfig(outDir);
      // update checker options
      checkerOptions.add("-A" + "serializationConfigPath=" + configPath);
      checkerOptions.add("-A" + "enableSerialization");
      // run checker
      super.run();
      Path expected = retrieveExpectedOutputPath();
      Path serialized = outDir.toPath().resolve("0").resolve("errors.json");
      verifyExpectedOutput(expected, serialized);
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
   * Verifies that the serialized output matches the expected output.
   *
   * @param expected the path to the expected output file.
   * @param serialized the path to the serialized output file.
   */
  private void verifyExpectedOutput(Path expected, Path serialized) {
    try {
      String serializedString = serialized.toFile().exists() ? new String(Files.readAllBytes(serialized)) : "";
      JSONObject serializedContent =
          serializedString.isEmpty()
              ? new JSONObject()
              : new JSONObject(
                  "{ \"errors\": ["
                      + serializedString.substring(0, serializedString.length() - 1)
                      + "]}");
      JSONObject expectedContent =
          expected.toFile().exists()
              ? new JSONObject(new String(Files.readAllBytes(expected)))
              : new JSONObject();
      if (!isEqualJSON(serializedContent, expectedContent)) {
        System.err.println("Expected: " + expectedContent.toString(2));
        System.err.println("Actual: " + serializedContent.toString(2));
        throw new AssertionError("Expected output does not match serialized output.");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  /**
   * Retrieves the expected output file for the test based on the test directory name.
   *
   * @return the path to the expected output file.
   */
  private Path retrieveExpectedOutputPath() {
    String currentDirectory = Paths.get("").resolve("tests").toAbsolutePath().toString();
    String singleSource = testFiles.get(0).toPath().toAbsolutePath().toString();
    return Paths.get(currentDirectory)
        .resolve(Paths.get(singleSource.substring(currentDirectory.length() + 1)).iterator().next())
        .resolve("expected-output.json");
  }

  /**
   * Compares two JSON objects. Ignores the "path" and "offset" fields.
   *
   * @param r1 JSON object to compare.
   * @param r2 JSON object to compare.
   * @return true if the two JSON objects are equal, false otherwise.
   */
  private static boolean isEqualJSON(Object r1, Object r2) {
    if (r1 == null || r2 == null) {
      return false;
    }
    if (r1.getClass() != r2.getClass()) {
      return false;
    }
    if (r1 instanceof JSONObject) {
      JSONObject json1 = (JSONObject) r1;
      JSONObject json2 = (JSONObject) r2;
      if (!json1.keySet().equals(json2.keySet())) {
        return false;
      }
      for (String key : json1.keySet()) {
        if (!key.equals("path") && !key.equals("offset")) {
          if (json1.get(key) instanceof JSONObject || json1.get(key) instanceof JSONArray) {
            if (!isEqualJSON(json1.get(key), json2.get(key))) {
              return false;
            }
          } else if (!json1.get(key).toString().equals(json2.get(key).toString())) {
            return false;
          }
        }
      }
    }
    if (r1 instanceof JSONArray) {
      JSONArray a1 = (JSONArray) r1;
      JSONArray a2 = (JSONArray) r2;
      if (a1.length() != a2.length()) {
        return false;
      }
      for (int i = 0; i < a1.length(); i++) {
        boolean found = false;
        Object target = a1.get(i);
        for (int j = 0; j < a2.length(); j++) {
          if (isEqualJSON(target, a2.get(j))) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    }
    return true;
  }
}

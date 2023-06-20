package edu.ucr.cs.riple.taint.ucrtainting;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.XMLUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Configuration class for the UCR Tainting project. */
public class Config {

  /**
   * Flag to specify the path to serialization config among the flags passed to checker framework.
   */
  public static final String SERIALIZATION_CONFIG_PATH = "serializationConfigPath";
  /**
   * Flag to specify whether serialization should be enabled or not among the flags passed to
   * checker framework.
   */
  public static final String SERIALIZATION_ACTIVATION_FLAG = "enableSerialization";
  /**
   * Path to serialization config. If {@link #serializationActivation} is false, it will be {@code
   * null}.
   */
  @Nullable public final Path configPath;
  /**
   * The directory where all output files will be written. If {@link #serializationActivation} is
   * false, it will be {@code null}.
   */
  @Nullable public final Path outputDir;
  /** Flag to control serialization internally. */
  public final boolean serializationActivation;

  public Config(UCRTaintingChecker checker) {
    this.serializationActivation = checker.hasOption(SERIALIZATION_ACTIVATION_FLAG);
    if (serializationActivation && !checker.hasOption(SERIALIZATION_CONFIG_PATH)) {
      throw new RuntimeException(
          "Please specify the serialization config using the flag: "
              + SERIALIZATION_CONFIG_PATH
              + " when enabling serialization.");
    }
    this.configPath =
        serializationActivation
            // Cannot call checker.getOption(SERIALIZATION_CONFIG_PATH), it throws an exception. Fix
            // later.
            ? Paths.get(checker.getOptions().get(SERIALIZATION_CONFIG_PATH))
            : null;
    String outputDirString = null;
    if (configPath != null) {
      Document document;
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(Files.newInputStream(configPath));
        document.normalize();
        outputDirString =
            XMLUtil.getValueFromTag(document, "/serialization/path", String.class).orElse(null);
      } catch (IOException | SAXException | ParserConfigurationException e) {
        throw new RuntimeException("Error in reading/parsing config at path: " + configPath, e);
      }
    }
    this.outputDir = outputDirString == null ? null : Paths.get(outputDirString);
  }

  /**
   * Returns whether serialization is enabled or not.
   *
   * @return true if serialization is enabled, false otherwise.
   */
  public boolean serializationEnabled() {
    return serializationActivation;
  }
}

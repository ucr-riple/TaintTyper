/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

  public final boolean enableLocalVariableOptimization;
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
    Document document = null;
    if (configPath != null) {
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
    this.enableLocalVariableOptimization =
        Boolean.TRUE.equals(
            XMLUtil.getValueFromTag(document, "/serialization/localVarOpt", Boolean.class)
                .orElse(true));
    System.out.println("Local Variable Optimization: " + this.enableLocalVariableOptimization);
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

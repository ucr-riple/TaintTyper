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

package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import static java.util.stream.Collectors.joining;

import com.google.common.base.Preconditions;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import edu.ucr.cs.riple.taint.ucrtainting.Config;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.json.JSONObject;

/**
 * Serializer class where all generated files in Fix Serialization package is created through APIs
 * of this class.
 */
public class Serializer {

  /** File name to write errors. */
  public static final String ERROR_OUTPUT = "errors.json";
  /** Config object used to configure the serializer. */
  private final Config config;
  /** Path to write errors. */
  private final @Nullable Path errorOutputPath;

  public Serializer(UCRTaintingChecker checker) {
    this.config = new Config(checker);
    this.errorOutputPath = config.outputDir != null ? config.outputDir.resolve(ERROR_OUTPUT) : null;
    initializeOutputFiles();
  }

  /**
   * Serializes the given error to the output file.
   *
   * @param error The error to serialize.
   */
  public void serializeError(Error error) {
    appendToFile(error.toJSON(), errorOutputPath);
  }

  /** Initializes every file which will be re-generated in the new run of checker. */
  private void initializeOutputFiles() {
    if (isDisabled()) {
      return;
    }
    Preconditions.checkArgument(errorOutputPath != null, "Unexpected null errorOutputPath");
    try {
      Preconditions.checkArgument(config.outputDir != null, "Unexpected null outputDir");
      Files.createDirectories(config.outputDir);
      try {
        Files.deleteIfExists(errorOutputPath);
        Files.write(
            config.outputDir.resolve("serialization_version.txt"),
            ("0\n" + config.disableLocalVariableOptimization).getBytes());
      } catch (IOException e) {
        throw new RuntimeException("Could not clear file at: " + errorOutputPath, e);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting serializer", e);
    }
  }

  /**
   * Appends the string representation of a json object.
   *
   * @param json The json object to append.
   */
  private void appendToFile(JSONObject json, Path path) {
    // Since there is no method available in API of either javac to inform checker
    // that the analysis is finished, we cannot open a single stream and flush it within a finalize
    // method. Must open and close a new stream everytime we are appending a new line to a file.
    if (json == null) {
      return;
    }
    json.put("index", UCRTaintingChecker.index + 1);
    String entry = json + ",\n";
    try (OutputStream os = new FileOutputStream(path.toFile(), true)) {
      os.write(entry.getBytes(Charset.defaultCharset()), 0, entry.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + path, e);
    }
  }

  /**
   * Converts the given uri to the real path.
   *
   * @param uri Given uri.
   * @return Real path for the give uri.
   */
  public static Path pathToSourceFileFromURI(URI uri) {
    if (uri == null) {
      return null;
    }
    if (!"file".equals(uri.getScheme())) {
      return null;
    }
    Path path = Paths.get(uri);
    try {
      return path.toRealPath();
    } catch (IOException e) {
      return path;
    }
  }

  /**
   * Serializes the given {@link Symbol} to a string.
   *
   * @param symbol The symbol to serialize.
   * @return The serialized symbol.
   */
  public static String serializeSymbol(@Nullable Symbol symbol) {
    if (symbol == null) {
      return "null";
    }
    switch (symbol.getKind()) {
      case FIELD:
      case PARAMETER:
        return symbol.name.toString();
      case METHOD:
      case CONSTRUCTOR:
        return serializeMethodSignature((Symbol.MethodSymbol) symbol);
      default:
        return symbol.flatName().toString();
    }
  }

  /**
   * Serializes the given method signature to a string.
   *
   * @param methodSymbol The method symbol to serialize.
   * @return The serialized method signature.
   */
  public static String serializeMethodSignature(Symbol.MethodSymbol methodSymbol) {
    StringBuilder sb = new StringBuilder();
    if (methodSymbol.isConstructor()) {
      // For constructors, method's simple name is <init> and not the enclosing class, need to
      // locate the enclosing class.
      Symbol.ClassSymbol encClass = methodSymbol.owner.enclClass();
      Name name = encClass.getSimpleName();
      if (name.isEmpty()) {
        // An anonymous class cannot declare its own constructor. Based on this assumption and our
        // use case, we should not serialize the method signature.
        throw new RuntimeException(
            "Did not expect method serialization for anonymous class constructor: "
                + methodSymbol
                + ", in anonymous class: "
                + encClass);
      }
      sb.append(name);
    } else {
      // For methods, we use the name of the method.
      sb.append(methodSymbol.getSimpleName());
    }
    sb.append(
        methodSymbol.getParameters().stream()
            .map(
                parameter ->
                    // check if array
                    (parameter.type instanceof Type.ArrayType)
                        // if is array, get the element type and append "[]"
                        ? ((Type.ArrayType) parameter.type).elemtype.tsym + "[]"
                        // else, just get the type
                        : parameter.type.tsym.toString())
            .collect(joining(",", "(", ")")));
    return sb.toString();
  }

  /**
   * Checks if the serialization is disabled.
   *
   * @return True if serialization is disabled, false otherwise.
   */
  public boolean isDisabled() {
    return !config.serializationEnabled();
  }

  /**
   * Logs the given message to the log file at /tmp/ucr_checker/log.txt.
   *
   * @param message The message to log.
   */
  public static void log(Object message) {
    final Path LOG_PATH = Paths.get("/tmp/ucr_checker/log.txt");
    try {
      if (!Files.exists(LOG_PATH.getParent())) {
        Files.createDirectories(LOG_PATH.getParent());
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not create directory at: " + LOG_PATH, e);
    }
    // append to file
    try (OutputStream os = new FileOutputStream(LOG_PATH.toFile(), true)) {
      os.write((message + "\n").getBytes(Charset.defaultCharset()), 0, (message + "\n").length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + LOG_PATH, e);
    }
  }
}

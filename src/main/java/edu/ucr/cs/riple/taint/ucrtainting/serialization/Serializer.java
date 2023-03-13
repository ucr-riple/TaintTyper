package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import static java.util.stream.Collectors.joining;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import edu.ucr.cs.riple.taint.ucrtainting.Config;
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

  public static final String ERROR_OUTPUT = "errors.json";

  private final Config config;

  public Serializer(Config config) {
    this.config = config;
    initializeOutputFiles();
  }

  /** Initializes every file which will be re-generated in the new run of checker. */
  private void initializeOutputFiles() {
    final Path errorOutputPath = config.outputDir.resolve(ERROR_OUTPUT);
    try {
      Files.createDirectories(config.outputDir);
      try {
        Files.deleteIfExists(errorOutputPath);
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
    String entry = json + "\n";
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
}

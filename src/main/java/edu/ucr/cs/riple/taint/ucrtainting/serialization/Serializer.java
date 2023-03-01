package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.tools.javac.code.Symbol;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serializer class where all generated files in Fix Serialization package is created through APIs
 * of this class.
 */
public class Serializer {

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
   * @param adapter adapter used to serialize symbols.
   * @return The serialized symbol.
   */
  public static String serializeSymbol(Symbol symbol) {
    // TODO: complete this once the output format is finalized.
    return "";
  }
}

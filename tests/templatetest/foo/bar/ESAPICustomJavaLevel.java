package foo.bar;

import java.util.logging.Level;

/**
 * Definitions of customized Java Logging Level options to map ESAPI behavior to the desired Java
 * Log output behaviors.
 */
public class ESAPICustomJavaLevel extends Level {

  protected static final long serialVersionUID = 1L;

  /**
   * Defines a custom error level below SEVERE but above WARNING since this level isn't defined
   * directly by java.util.Logger already.
   */
  public static final Level ERROR_LEVEL =
      new ESAPICustomJavaLevel("ERROR", Level.SEVERE.intValue() - 1);

  /**
   * Defines a custom level that should result in content always being recorded, unless the Java
   * Logging configuration is set to OFF.
   */
  public static final Level ALWAYS_LEVEL =
      new ESAPICustomJavaLevel("ALWAYS", Level.OFF.intValue() - 1);

  /**
   * Constructs an instance of a JavaLoggerLevel which essentially provides a mapping between the
   * name of the defined level and its numeric value.
   *
   * @param name The name of the JavaLoggerLevel
   * @param value The associated numeric value
   */
  private ESAPICustomJavaLevel(String name, int value) {
    super(name, value);
  }
}

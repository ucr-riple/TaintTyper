import java.util.logging.Logger;

/**
 * Contract used to isolate translations for each Java Logging Level.
 *
 * @see JavaLogLevelHandlers
 * @see JavaLogBridgeImpl
 */
interface JavaLogLevelHandler {
  /** Check if the logging level is enabled for the specified logger. */
  boolean isEnabled(Logger logger);
  /**
   * Calls the appropriate log level event on the specified logger.
   *
   * @param logger Logger to invoke.
   * @param msg Message to log.
   */
  void log(Logger logger, String msg);
  /**
   * Calls the appropriate log level event on the specified logger.
   *
   * @param logger Logger to invoke
   * @param msg Message to log
   * @param th Throwable to log.
   */
  void log(Logger logger, String msg, Throwable th);
}

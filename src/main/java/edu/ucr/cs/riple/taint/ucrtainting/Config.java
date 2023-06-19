package edu.ucr.cs.riple.taint.ucrtainting;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuration class for the UCR Tainting project. */
public class Config {

  /** Flag to specify the output directory among the flags passed to checker framework. */
  public static final String OUTPUT_DIR_FLAG = "outputDir";
  /**
   * Flag to specify whether serialization should be enabled or not among the flags passed to
   * checker framework.
   */
  public static final String SERIALIZATION_ACTIVATION_FLAG = "enableSerialization";
  /**
   * The directory where all output files will be written. If not specified, defaults to
   * /tmp/ucr-tainting/0.
   */
  public final Path outputDir;
  /** Flag to control serialization internally. */
  public final boolean serializationActivation;

  public Config(UCRTaintingChecker checker) {
    this.serializationActivation = checker.hasOption(SERIALIZATION_ACTIVATION_FLAG);
    if (checker.hasOption(OUTPUT_DIR_FLAG)) {
      this.outputDir = Paths.get(checker.getOption(OUTPUT_DIR_FLAG));
    } else {
      this.outputDir = Paths.get("/tmp/ucr-tainting/0");
    }
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

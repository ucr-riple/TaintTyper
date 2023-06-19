package edu.ucr.cs.riple.taint.ucrtainting;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuration class for the UCR Tainting project. */
public class Config {

  public static final String OUTPUT_DIR_FLAG = "outputDir";
  public static final String SERIALIZATION_ACTIVATION_FLAG = "enableSerialization";

  public final Path outputDir;
  public final boolean serializationActivation;

  public Config(UCRTaintingChecker checker) {
    this.serializationActivation = checker.hasOption(SERIALIZATION_ACTIVATION_FLAG);
    if (checker.hasOption(OUTPUT_DIR_FLAG)) {
      this.outputDir = Paths.get(checker.getOption(OUTPUT_DIR_FLAG));
    } else {
      this.outputDir = Paths.get("/tmp/ucr-tainting/0");
    }
  }

  public boolean serializationEnabled() {
    return true;
  }
}

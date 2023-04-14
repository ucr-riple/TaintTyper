package edu.ucr.cs.riple.taint.ucrtainting;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuration class for the UCR Tainting project. */
public class Config {

  public final Path outputDir;

  public Config() {
    this.outputDir = Paths.get("/tmp/ucr-tainting/0");
  }
}

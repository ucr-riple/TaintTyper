package edu.ucr.cs.riple.taint.ucrtainting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class Log {

  private static final String fileName = "log.txt";

  public static void log(String message) {
    Path path = Paths.get("/tmp/ucr-tainting/0").resolve(fileName);
    try {
      if (!path.toFile().exists()) {
        Files.createFile(path);
      }
      Files.write(path, (message + "\n").getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void print(String message) {
    System.err.println("LOG: " + message);
  }

  public Stream<String> testOnStreamLambda(List<String> s) {
    return s.stream().filter(x -> x.length() > 0);
  }
}

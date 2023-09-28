package edu.ucr.cs.riple.taint.ucrtainting;

import java.util.List;
import java.util.stream.Stream;

public class Log {

  private static final String fileName = "log.txt";

  public static void print(String message) {
    System.err.println("LOG: " + message);
  }

  public Stream<String> testOnStreamLambda(List<String> s) {
    return s.stream().filter(x -> x.length() > 0);
  }
}

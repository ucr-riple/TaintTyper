package edu.ucr.cs.riple.taint.ucrtainting;

public class Log {

  private static final String fileName = "log.txt";

  public static void print(String message) {
    System.err.println(message);
  }
}

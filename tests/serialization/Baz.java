package foo.bar;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;

class Baz {
  public String field = "";

  public static String staticF = "";

  String getField() {
    return field;
  }
}

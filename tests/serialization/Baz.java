import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

class Baz {
  public String field = "";

  public static String staticF = "";

  String getField() {
    return field;
  }
}

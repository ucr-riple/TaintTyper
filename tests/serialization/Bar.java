import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

class Bar {
  public String field = "";

  public Baz baz = new Baz();

  public static String staticF = "";

  String getField() {
    return field;
  }
}

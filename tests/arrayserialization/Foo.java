import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Foo {

  Object field;

  public void bar(Object param) {
    // :: error: array.initializer
    // :: error: assignment
    @RUntainted Object[] arr = {field, param};
  }
}

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Foo {

  Object field;

  public void bar(Object param) {
    // :: error: array.initializer
    // :: error: assignment
    @RUntainted Object[] arr = {field, param};
  }

  public void arrayCopy(String[] existedProperties) {
    int epl = existedProperties.length;
    // :: error: assignment
    @RUntainted String[] newProperties = Arrays.copyOf(existedProperties, epl + 1);
  }

  public void toArrayTest(Bar<String, @RUntainted String, String> b) {
    @RUntainted String[] processProperties = b.toArray(new String[b.size()]);
  }

  static class CustomList<E, N> extends ArrayList<N> {}

  static class Bar<P, Q, R> extends CustomList<R, Q> {}
}

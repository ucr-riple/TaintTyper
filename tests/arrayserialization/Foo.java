import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

  public void toArrayTest(
      Bar<@RUntainted String, @RUntainted String, @RUntainted String> processPropList) {
    @RUntainted
    String[] processProperties = processPropList.toArray(new String[processPropList.size()]);
  }

  public void toArrayTestNoError(Bar<String, @RUntainted String, String> b) {
    @RUntainted String[] processProperties = b.toArray(new String[b.size()]);
  }

  public void toArrayFromMethodCall(Map<String, Bar<String, @RUntainted String, String>> b) {
    @RUntainted
    String[] processProperties = b.values().iterator().next().toArray(new String[b.size()]);
  }

  public void toArrayTestWithError(Map<String, Bar<@RUntainted String, String, String>> b) {
    @RUntainted
    // :: error: assignment
    String[] processProperties = b.values().iterator().next().toArray(new String[b.size()]);
  }

  public void toArrayTestWithErrorSimple(List<String> list) {
    @RUntainted
    // :: error: assignment
    String[] processProperties = list.toArray(new String[list.size()]);
  }

  static class CustomList<E, N> extends ArrayList<N> {}

  static class Bar<P, Q, R> extends CustomList<R, Q> {}
}

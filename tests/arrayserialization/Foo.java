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

  public void toArrayTest(
      Bar<@RUntainted String, @RUntainted String, @RUntainted String> processPropList) {
    // :: error: assignment
    @RUntainted
    String[] processProperties = processPropList.toArray(new String[processPropList.size()]);
  }

  class CustomList<T, K> extends ArrayList<K> {}

  class Bar<M, N, L> extends CustomList<N, M> {}
}

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

class Foo {
  public void test1(Map<String, String> map) {
    // :: error: assignment
    Collection<@RUntainted String> s = map.values();
  }
}

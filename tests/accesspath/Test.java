import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

  HashMap<String, String> foo;
  Inner inner;

  void bar() {
    // :: error: assignment
    @RUntainted String a = foo.keySet().iterator().next();
    // :: error: assignment
    @RUntainted String b = inner.bar.keySet().iterator().next();
    // :: error: assignment
    @RUntainted String c = inner.test.foo.keySet().iterator().next();
    // :: error: assignment
    @RUntainted String d = inner.test.inner.bar.keySet().iterator().next();
  }

  class Inner {

    HashMap<String, String> bar;
    Test test;
  }
}

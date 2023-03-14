import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

  public HashMap<String, String> foo;
  Inner inner;

  void bar() {
    // :: error: assignment
    @RUntainted String a = foo.keySet().iterator().next();
    // :: error: assignment
    @RUntainted String b = inner.bar.keySet().iterator().next();
  }

  class Inner {
    public HashMap<String, String> bar;
  }
}

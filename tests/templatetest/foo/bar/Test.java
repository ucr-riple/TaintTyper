package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.List;

public class Test {

  Test(List<String> param) {
    List<String> l = param;
    String s = l.get(0);
    // :: error: argument
    sink(s);
  }

  void sink(@RUntainted String param) {}
}

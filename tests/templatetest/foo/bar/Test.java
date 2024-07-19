package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {

  Test(Object param) {}

  void test(Object param) {
    // :: error: assignment
    @RUntainted Test t = new Test(null);
  }

  static String method() {
    return null;
  }
}

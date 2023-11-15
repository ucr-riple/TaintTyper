package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.List;

public class Test {

  public void test0() {
    List<String> lines = new ArrayList<>();
    // :: error: enhancedfor
    for (java.lang.@RUntainted String line : lines) {}
  }

  public void test1(@RUntainted ArrayList<String> lines) {
    // :: error: enhancedfor
    for (java.lang.@RUntainted String line : lines) {}
  }
}

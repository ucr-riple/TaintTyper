package stringBuilderTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;

// Test taint passing through custom library method invocation
class StringBuilderTest {
  @RTainted String fieldX;

  //  void tainted() {
  //    StringBuilder content = new StringBuilder();
  //    content.append("<fieldX>").append(fieldX);
  //    sink(content.toString());
  //  }

  void tainted2(@RTainted String arg) {
    StringBuffer content = new StringBuffer();
    content.append(fieldX).append(arg);
    sink(content.toString());
  }

  void sink(@RUntainted String str) {}
}

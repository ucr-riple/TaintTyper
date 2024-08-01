package stringBuilderTests;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RTainted;
import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RUntainted;

// Test taint passing through custom library method invocation
class StringBuilderTest {
  @RTainted String fieldX;

  //  void tainted() {
  //    StringBuffer content = new StringBuffer();
  //    content.append("fieldX").append(fieldX).append("random").append("more random");
  //    // :: error: (argument)
  //    sink(content.toString());
  //  }
  //
  //  void tainted2(@RTainted String arg) {
  //    StringBuffer content = new StringBuffer();
  //    content.append("fieldX").append(arg);
  //    // :: error: (argument)
  //    sink(content.toString());
  //  }
  //
  //  void tainted3() {
  //    StringBuffer content = new StringBuffer();
  //    content.append("fieldX").append("random").append("more random").append(fieldX);
  //    // :: error: (argument)
  //    sink(content.toString());
  //  }
  //
  //  void tainted4() {
  //    StringBuffer content = new StringBuffer();
  //    content.append("fieldX").append("random").append("more random");
  //    sink(content.toString());
  //  }

  void sink(@RUntainted String str) {}
}

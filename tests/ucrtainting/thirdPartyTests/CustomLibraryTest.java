package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import thirdPartyTests.com.test.thirdparty.LibraryCodeTestSupport;

// Test taint passing through custom library method invocation
class CustomLibraryTest {
  void untainted(@RUntainted String y) {
    LibraryCodeTestSupport dummy = new LibraryCodeTestSupport(y);
    @RUntainted String dummyStr = dummy.getVal();
  }

  void tainted(@RTainted String y) {
    LibraryCodeTestSupport dummy = new LibraryCodeTestSupport(y);
    // :: error: assignment
    @RUntainted String dummyStr = dummy.getVal();
  }

  void newClassTestTaintedSink(@RTainted String y) {
    // :: error: argument
    testSink(new LibraryCodeTestSupport(y).getVal());
  }

  void newClassTestUntaintedSink(@RUntainted String y) {
    testSink(new LibraryCodeTestSupport(y).getVal());
  }

  void testSink(@RUntainted String y) {
    @RTainted String dummyStr = y;
  }
}

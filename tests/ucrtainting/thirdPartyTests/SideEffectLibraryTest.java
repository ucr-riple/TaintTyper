package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import thirdPartyTests.com.test.thirdparty.LibraryCodeTestSupport;

// Test taint passing through custom library method invocation
class SideEffectLibraryTest {

  void tainted(@RTainted String y, @RUntainted String x) {
    LibraryCodeTestSupport dummy = new LibraryCodeTestSupport(x);
    dummy.setVal(y);
    // :: error: (assignment)
    @RUntainted String dummyStr = dummy.getVal();
  }

  void unTainted(@RTainted String y, @RUntainted String x) {
    LibraryCodeTestSupport dummy = new LibraryCodeTestSupport(x);
    @RUntainted String dummyStr = dummy.getVal();
  }
}

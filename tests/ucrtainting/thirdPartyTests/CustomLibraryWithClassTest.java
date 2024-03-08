package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import thirdPartyTests.com.test.thirdparty.LibraryCodeTestSupport;

class CustomLibraryWithClassTest {
  void test(@RUntainted Object o) {
    // :: error: assignment
    @RUntainted String dummyStrFailing = LibraryCodeTestSupport.singleTon.getVal(String.class, o);

    @RUntainted String dummyStrPassing = LibraryCodeTestSupport.singleTon.getVal(String.class, o, true);
  }
}

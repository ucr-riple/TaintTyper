package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import java.util.List;
import thirdPartyTests.com.test.thirdparty.LibraryCodeTestSupport;

class CustomLibraryWithClassTest {
  void test(@RUntainted Object o) {
    // :: error: assignment
    @RUntainted String dummyStrFailing = LibraryCodeTestSupport.singleTon.getVal(String.class, o);

    @RUntainted
    String dummyStrPassing = LibraryCodeTestSupport.singleTon.getVal(String.class, o, true);
  }

  List<@RUntainted String> testList1(@RUntainted String s) {
    return LibraryCodeTestSupport.singleTon.list(s);
  }

  void testList2(@RUntainted String s) {
    List<@RUntainted String> resList = LibraryCodeTestSupport.singleTon.list(s);
  }
}

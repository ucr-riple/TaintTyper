package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import thirdPartyTests.com.test.thirdparty.LibraryCodeTestSupport;

import java.util.List;

// Test taint passing through custom library method invocation
class CustomLibraryUntaintedTypeParam {
    public void test(List<@RUntainted String> s) {
        LibraryCodeTestSupport.singleTon.typeArgSinkTester(s);
    }

}

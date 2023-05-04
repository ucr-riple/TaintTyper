package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import org.apache.commons.lang3.StringUtils;

// Test taint passing through library method invocation
class LibraryInvocationTest {
  void untaintedToUntainted(@RUntainted String y) {
    @RUntainted String z = StringUtils.capitalize(y);
  }

  void untaintedToTainted(@RUntainted String y) {
    @RTainted String z = StringUtils.capitalize(y);
  }

  void taintedToUntainted(@RTainted String y) {
    // :: error: (assignment)
    @RUntainted String z = StringUtils.capitalize(y);
  }

  void taintedToTainted(@RTainted String y) {
    @RTainted String z = StringUtils.capitalize(y);
  }

  int foo() {
    return 0;
  }
}

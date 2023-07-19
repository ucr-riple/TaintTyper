package validatorTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;

// Test basic subtyping relationships for the UCR Tainting Checker.
class ValidatorTest {
  void validationArg(@RTainted String y) {
    if (validator(y)) {
      sink(y);
    }
    sink(y);
  }

  void validationReceiver(@RTainted String y) {
    if(y.equals("ssc")) {
      return;
    }
    if (y.contains("ss")) {
      return;
    }
    sink2(y);
  }

  void sink2(@RPossiblyValidated({"y.equals(\"ssc\")", "y.contains(\"ss\")"}) String s) {}
  void sink(@RPossiblyValidated({"(this).validator(y)"}) String s) {}

  boolean validator(@RTainted String a) {
    return false;
  }
}

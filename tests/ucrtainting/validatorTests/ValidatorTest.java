package validatorTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;

// Test basic subtyping relationships for the UCR Tainting Checker.
class ValidatorTest {

  void subTypeTest1(@RTainted String x, @RPossiblyValidated({}) String z) {
    // :: error: (assignment)
    z = x;
  }

  void subTypeTest2(@RUntainted String x, @RPossiblyValidated({}) String z) {
    z = x;
  }

  void subTypeTest3(@RTainted String x, @RPossiblyValidated({}) String z) {
    x = z;
  }

  void subTypeTest4(@RPossiblyValidated({"a", "b"}) String x, @RPossiblyValidated({"a"}) String y) {
    // :: error: (assignment)
    x = y;
  }

  void subTypeTest5(@RPossiblyValidated({"a"}) String y, @RPossiblyValidated({}) String z) {
    // :: error: (assignment)
    y = z;
  }

  void subTypeTest6(@RPossiblyValidated({"a", "b"}) String x, @RPossiblyValidated({}) String z) {
    z = x;
  }

  void validationArg(@RTainted String y) {
    if (validator(y)) y.isEmpty();
    sink(y);
  }

  void validationReceiver(@RTainted String y) {
    if (y.equals("ssc")) {
      return;
    }
    if (y.contains("ss")) {
      return;
    }
    sink2(y);
  }

  void validationNested(@RTainted String y) {
    if (y.contains("r")) {
      if (y.equals("y")) {
        sink3(y);
      } else {
        if (y.equals("z")) {
          sink4(y);
        }
      }
      sink5(y);
    } else if (y.contains("something")) {
      return;
    }
    sink6(y);
  }

  void sink(@RPossiblyValidated({"(this).validator(y)"}) String s) {}

  void sink2(@RPossiblyValidated({"y.equals(\"ssc\")", "y.contains(\"ss\")"}) String s) {}

  void sink3(@RPossiblyValidated({"y.contains(\"r\")", "y.equals(\"y\")"}) String s) {}

  void sink4(
      @RPossiblyValidated({"y.contains(\"r\")", "y.equals(\"y\")", "y.equals(\"z\")"}) String s) {}

  void sink5(@RPossiblyValidated({"y.contains(\"r\")", "y.equals(\"y\")"}) String s) {}

  void sink6(@RPossiblyValidated({"y.contains(\"r\")"}) String s) {}

  boolean validator(@RTainted String a) {
    return false;
  }
}

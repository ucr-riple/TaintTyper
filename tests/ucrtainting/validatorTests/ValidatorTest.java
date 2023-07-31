package validatorTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;

// Test basic subtyping relationships for the UCR Tainting Checker.
class ValidatorTest {
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

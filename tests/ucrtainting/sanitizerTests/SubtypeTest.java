package sanitizerTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import org.apache.commons.text.StringEscapeUtils;

// Test basic subtyping relationships for the UCR Tainting Checker.
class SubtypeTest {
  void allSubtypingRelationships(@RTainted String y) {
    sink(StringEscapeUtils.escapeEcmaScript(y));
    if(validator(y)) {
      sink(y);
    }
    sink(y);
  }

  void sink(@RUntainted String s) {}

  boolean validator(@RTainted String a) {
    return false;
  }
}

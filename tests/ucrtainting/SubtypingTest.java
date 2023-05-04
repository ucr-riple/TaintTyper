import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

// Test basic subtyping relationships for the UCR Tainting Checker.
class SubtypeTest {
  void allSubtypingRelationships(int x, @RUntainted int y) {
    @RTainted int a = x;
    @RTainted int b = y;
    // :: error: assignment
    @RUntainted int c = x; // expected error on this line
    @RUntainted int d = y;
  }
}

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

// Test basic subtyping relationships for the UCR Tainting Checker.
class SubtypeTest {
    void allSubtypingRelationships(int x, @Untainted int y) {
        @Tainted int a = x;
        @Tainted int b = y;
        // :: error: assignment
        @Untainted int c = x; // expected error on this line
        @Untainted int d = y;
    }
}

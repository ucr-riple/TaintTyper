import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

// Test basic subtyping relationships for the UCR Tainting Checker.
class SubtypeTest {
    void allSubtypingRelationships(@UCRTaintingUnknown int x, @UCRTaintingBottom int y) {
        @UCRTaintingUnknown int a = x;
        @UCRTaintingUnknown int b = y;
        // :: error: assignment
        @UCRTaintingBottom int c = x; // expected error on this line
        @UCRTaintingBottom int d = y;
    }
}

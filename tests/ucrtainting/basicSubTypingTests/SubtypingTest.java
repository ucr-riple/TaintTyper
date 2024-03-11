package basicSubTypingTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Test basic subtyping relationships for the UCR Tainting Checker.
class SubtypeTest {
  void allSubtypingRelationships(int x, @RUntainted int y) {
    @RTainted int a = x;
    @RTainted int b = y;
    // :: error: assignment
    @RUntainted int c = x; // expected error on this line
    @RUntainted int d = y;
  }

  public void polyTester(@RTainted String s) {
    // :: error: (assignment)
    @RUntainted String untaintedVal = polyTest(s);
    @RTainted String taintedVal = polyTest(s);
  }

  public @RPolyTainted String polyTest(@RPolyTainted String arg) {
    return arg;
  }

  public void createTempDirFP(@RUntainted File f) throws IOException {
    // :: error: assignment
    @RUntainted Path path = Files.createTempDirectory(f.toPath(), "random-suffx");
  }
}

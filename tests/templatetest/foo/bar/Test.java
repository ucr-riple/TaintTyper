package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.awt.Point;

public class Test {
  Point loc;

  //  public void testFix(Point p) {
  //    // :: error: assignment
  //    @RUntainted int i = p.x;
  //  }

  public void testError(@RUntainted Point p) {
    @RUntainted int i = p.x;
  }
}

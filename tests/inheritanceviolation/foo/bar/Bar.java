package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Bar extends Foo {

  // :: error: (override.return)
  public Object getUntainted() {
    return null;
  }

  // :: error: (override.param)
  public void getUntainted(@RUntainted Object pBar) {
    return;
  }
}

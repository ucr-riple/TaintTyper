package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo {

  public @RUntainted Object getUntainted() {
    return null;
  }

  public void getUntainted(Object pFoo) {
    return;
  }
}

package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

  public <T> T getVal(Class<T> c, Object o) {
    return c.cast(o);
  }
}

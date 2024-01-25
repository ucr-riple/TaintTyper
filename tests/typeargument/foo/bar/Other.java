package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Other {
  TypeArgument<
          String,
          String,
          Map<String, String>,
          HashMap<String, String>,
          HashMap<HashMap<String, String>, Map<?, String>>>
      o;

  Inner inner;

  public TypeArgument<
          String,
          String,
          Map<String, String>,
          HashMap<String, String>,
          HashMap<HashMap<String, String>, Map<?, String>>>
      getO() {
    return o;
  }

  class Inner {
    TypeArgument<
            String,
            String,
            Map<String, String>,
            HashMap<String, String>,
            HashMap<HashMap<String, String>, Map<?, String>>>
        innerField;
  }
}

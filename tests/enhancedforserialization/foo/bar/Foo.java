package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.HashMap;
import java.util.Map;

public class Foo {
  public void testFor() {
    Map<String, String> headers = new HashMap<>();
    // :: error: enhancedfor
    for (Map.Entry<@RUntainted String, @RUntainted String> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
    }
  }
}

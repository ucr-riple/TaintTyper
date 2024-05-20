package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Foo {

  @SuppressWarnings("unchecked")
  public void testNewCollectionAsArgumentForRawType(Map map) {
    // We expect that checker must not crash here
    List<String> ret = new ArrayList<String>(map.keySet());
  }
}

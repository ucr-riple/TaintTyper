package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;

public class Foo {

  @SuppressWarnings("unchecked")
  public void testNewCollectionAsArgumentForRawType(Map map) {
    // We expect that checker must not crash here
    List<String> ret = new ArrayList<String>(map.keySet());
  }

  void acknowledgeAnnotOnReceiverOnToArrayMethod() {
    LinkedList<@RUntainted String> c1 = new LinkedList<>();
    @RUntainted Object[] array = c1.toArray();
  }

  void refraingFromApplyingUnannotatedCodeHandlerForToArrayMethod() {
    LinkedList<String> c1 = new LinkedList<>();
    // :: error: assignment
    @RUntainted Object[] array = c1.toArray();
  }
}

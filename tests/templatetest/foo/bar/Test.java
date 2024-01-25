package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

      private List<String> tokens;
      public List<@RUntainted String> getAllTokens() {
          // :: error: return
          return this.tokens;
      }

  //    public void enhancedForLoopOnList(@RUntainted Object action, List<Method> methods) {
  //        // :: error: enhancedfor
  //        for (@RUntainted Method m : methods) {
  //            try {
  //                MethodUtils.invokeMethod(action, true, m.getName());
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        }
  //    }
}

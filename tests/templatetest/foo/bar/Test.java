package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {

  public void test(ActionInvocation action, List<Method> methods) {
    for (Method m : methods) {
      final String resultCode = (String) MethodUtils.invokeMethod(action, true, m.getName());
    }
  }
}

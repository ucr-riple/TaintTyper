package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.commons.lang3.reflect.MethodUtils;

public class Foo {

  private final JavaLogLevelHandlers javaLogLevelHandlers = JavaLogLevelHandlers.SEVERE;

  public void enhancedForLoopOnList(@RUntainted Object action) {
    List<@RUntainted Method> methods =
        new ArrayList<@RUntainted Method>(
            MethodUtils.getMethodsListWithAnnotation(
                action.getClass(), BeforeResult.class, true, true));
    for (@RUntainted Method m : methods) {
      try {
        MethodUtils.invokeMethod(action, true, m.getName());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @interface BeforeResult {
    int priority() default 10;
  }
}

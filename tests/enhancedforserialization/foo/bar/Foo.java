package foo.bar;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.reflect.MethodUtils;

public class Foo {

  public void testFor() {
    Map<String, String> headers = new HashMap<>();
    // :: error: enhancedfor
    for (Map.Entry<@RUntainted String, @RUntainted String> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
    }
  }

  public void enhancedForLoopOnList(@RUntainted Object action) {
    List<Method> methods =
        new ArrayList<Method>(
            MethodUtils.getMethodsListWithAnnotation(action.getClass(), null, true, true));
    // :: error: enhancedfor
    for (@RUntainted Method m : methods) {
      try {
        MethodUtils.invokeMethod(action, true, m.getName());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void enhancedForLoopCollectionSupport() {
    final @RUntainted Collection<String> files = new ArrayList<>();
    // :: error: (enhancedfor)
    for (final @RUntainted String f : files) {}
  }

  public void enhancedForLoopOnMethodReturnTypeTest() {
    // :: error: enhancedfor
    for (final @RUntainted String paramcls : getParameterClasses()) {}
  }

  public Collection<String> getParameterClasses() {
    return null;
  }

  public void wildCardDeclaredTest(Map<String, ?> map) {
    // :: error: (enhancedfor)
    for (@RUntainted Object o : map.values()) {}
  }

  public void enhancedForLoopMixedWithTypeVar() {
    final Map<Class<? extends Annotation>, @RUntainted Collection<String>> matchingInterfaces =
        new HashMap<>();
    for (Map.Entry<Class<? extends Annotation>, @RUntainted Collection<String>> e :
        matchingInterfaces.entrySet()) {
      // :: error: enhancedfor
      for (@RUntainted String intName : e.getValue()) {}
    }
  }

  private void enhancedForLoopMixedWithTypeVarNested(
      List<Pair<Integer, Integer>> quotedRegionIndices) {
    for (Pair<@RUntainted Integer, @RUntainted Integer> nextQuotedRegionIndices :
        // :: error: enhancedfor
        quotedRegionIndices) {}
  }

  public final class Pair<F, S> {}

  public void testCustomList(Custom<String, String> custom) {
    // :: error: enhancedfor
    for (@RUntainted String s : custom) {}
  }

  private static class Custom<E, H_I> extends ArrayList<H_I> {}

  public void updateFoundRequiredTypeInAssignmentScannerTest(List<String> list) {
    for (String s : list) {
      // :: error: argument
      sink(s);
    }
  }

  public void testLocalVariablesNestedAssignmentWithEnhancedForLoop(
      Map<Map<List<String>, String>, List<String>> map) {
    for (Map<List<String>, String> smallMap : map.keySet()) {
      for (Map.Entry<List<String>, String> entry : smallMap.entrySet()) {
        for (String s : entry.getKey()) {
          // :: error: argument
          sink(s);
        }
      }
    }
  }

  public void sink(@RUntainted String s) {}
}

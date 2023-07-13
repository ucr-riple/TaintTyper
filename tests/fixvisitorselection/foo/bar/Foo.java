package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo {

  //  public void testClassTypeWithArg() {
  //    Bar<String, String> bar = new Bar<String, String>();
  //    // :: error: assignment
  //    Map<@RUntainted String, @RUntainted String> map = bar.getMapWithE();
  //    // :: error: assignment
  //    @RUntainted String s = bar.getE();
  //  }
  //
  //  public void defaultVisitor() {
  //    Bar<String, String> bar = new Bar<String, String>();
  //    // :: error: assignment
  //    @RUntainted String s = bar.getString();
  //  }
  //
  //  public void testThirdPartyCall(
  //      StringSubstitutorReader reader,
  //      final char[] target,
  //      final int targetIndexIn,
  //      final int targetLengthIn)
  //      throws Exception {
  //    // :: error: assignment
  //    @RUntainted int ans = reader.read(target, targetIndexIn, targetLengthIn);
  //  }
  //
  //  public void testWithTypeArgFix() {
  //    Map<String, String> map = new HashMap<String, String>();
  //    // :: error: assignment
  //    @RUntainted String s = map.get("foo");
  //  }

  public HashMap<String, String> getMap() {
    return null;
  }

  public void testWithMethodWithTypeArgs() {
    // :: error: assignment
    Map<String, @RUntainted String> map = unmodifiable(getMap());
  }

  public <M, L> HashMap<M, L> unmodifiable(Map<? extends M, ? extends L> m) {
    return null;
  }
  //
  //  public void test(@RUntainted String param){
  //
  //  }
  //
  //  public void wrong(String p){
  //    // :: error: argument
  //    test(p);
  //  }
}

class Bar<T, E> {

  public Map<String, E> getMapWithE() {
    return null;
  }

  public E getE() {
    return null;
  }

  public String getString() {
    return null;
  }
}

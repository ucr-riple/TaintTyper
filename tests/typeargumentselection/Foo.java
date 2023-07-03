package test;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;

public class Foo {

  //  public void testMap(String param) {
  //    Map<String, String> map = new HashMap<>();
  //    // :: error: assignment
  //    @RUntainted String s = map.get(param);
  //  }
  //
  //    public void testMapOfList(List<String> key) {
  //      Map<List<String>, List<String>> mapOfList = new HashMap<>();
  //      // :: error: assignment
  //      @RUntainted String s = mapOfList.get(key).get(0);
  //    }

  public void testGenericFoo() {
    GenericFoo<String, String> gen = new GenericFoo<>();
    // :: error: assignment
    @RUntainted String s = gen.bar.rand();
  }

  static class GenericFoo<T, R> {
    GenericBar<T> bar;

    GenericBar<T> getBar() {
      return bar;
    }
  }

  static class GenericBar<T> {
    T rand() {
      return null;
    }
  }
}

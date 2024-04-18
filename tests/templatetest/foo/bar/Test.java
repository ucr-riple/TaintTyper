package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.Map;
import java.util.List;

public class Test {

    Baz<String> baz;

    public void test() {
      // :: error: assignment
      @RUntainted String untaintedVar = baz.foo.getE();
    }
   }

   class Baz<E> {
    Foo<E> foo;
   }

   class Foo<E> {

    public Map<E, String> map;

    public E getE() {
      return map.keySet().iterator().next();
    }

    public String getString() {
      return "";
    }
}

//  public void test(Map<String, List<String>> map) {
//    // :: error: assignment
//    @RUntainted String s = map.get("f").get(0);
//  }
//}

//  public void test(Foo<List<String>, String> foo) {
//    // :: error: assignment
//    Bar<String, List<@RUntainted List<@RUntainted String>>> s = foo.getBar();
//  }
// Bar<String, List<@RUntainted List<@RUntainted String>>> - getBar()
// -> M -> String , K -> @Runtainted List<@RUntaintd String>

// class Foo<K, M> {
//
//  Bar<M, List<K>> getBar() {
//    return null;
//  }
// }
//
// class Bar<T, U> {
//  T t;
//  U u;
//
//  T getT() {
//    return t;
//  }
//
//  U getU() {
//    return u;
//  }
// }

package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
// import org.thirdparty.Foo;
import java.util.Map;

public class Test {

  Foo<String, String> foo;
  Bar<String, String> bar;

  class Foo<K, M> {
    Map<K, M> getMap() {
      return null;
    }
  }

  class Bar<K, M> {
    Map<String, String> getMap() {
      return null;
    }
  }

  public void test() {
    //        // :: error: assignment
    //        @RUntainted String s0 = foo.getMap().keySet().iterator().next();
    // :: error: assignment
    @RUntainted String s1 = bar.getMap().keySet().iterator().next();
  }

  //    Foo<String> foo;
  //
  //    public void test(){
  //        // :: error: assignment
  //        @RUntainted String s = foo.getMap().get("f");
  //    }
  //
  //    Baz<String> baz;
  //
  //    public void test() {
  //      // :: error: assignment
  //      @RUntainted String untaintedVar = baz.foo.getE();
  //    }
  //   }
  //
  //   class Baz<E> {
  //    Foo<E> foo;
  //   }
  //
  //   class Foo<E> {
  //
  //    public Map<E, String> map;
  //
  //    public E getE() {
  //      return map.keySet().iterator().next();
  //    }
  //
  //    public String getString() {
  //      return "";
  //    }
  //
  //
  //    Map<String, String> map;
  //    Inner i;
  //    MyMap<String, String> myMap;
  //
  //    public void test(){
  //        // :: error: assignment
  //        @RUntainted String s = i.test.map.keySet().iterator().next();
  //    }
  //
  //    public void test1(){
  //        // :: error: assignment
  //        @RUntainted String s = i.test.myMap.m1.keySet().iterator().next();
  //    }
  //
  //
  //    class Inner {
  //      Test test;
  //    }
  //
  //    class MyMap<K, V>{
  //        Map<K, V> m1;
  //    }

  //    Baz<String> baz;
  //
  //    public void test(){
  //        // :: error: assignment
  //        @RUntainted String s = baz.getMap().keySet().iterator().next();
  //    }
  //
  //
  //    class Baz<E>{
  //        Map<String, E> getMap(){
  //            return null;
  //        }
  //    }

}

//  public void test(Map<String, List<String>> map) {
//    // :: error: assignment
//    @RUntainted String s = map.get("f").get(0);
//  }
// }

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

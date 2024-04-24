package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo<E, D> {

  private final Foo<String, String> withString = null;
  private final Foo<E, D> withType = null;

  public Foo<E, D> getWithType() {
    return withType;
  }

  public Foo<String, String> getWithString() {
    return withString;
  }

  public void onTypeParameter1(Foo<@RUntainted ?, ?> p) {}

  public void onTypeParameter2(Foo<?, @RUntainted ?> p) {}

  public void onTypeParameterAll(Foo<@RUntainted ?, @RUntainted ?> p) {}

  public void onParameter(@RUntainted Foo<?, ?> p) {}

  public void coreTest() {
    // :: error: argument
    onTypeParameter1(getWithType());
    // :: error: argument
    onTypeParameter2(getWithType());
    // :: error: argument
    onTypeParameterAll(getWithType());
    // :: error: argument
    onParameter(getWithType());
  }

  public void testOnTypeParameter(Foo<String, String> param) {
    // :: error: argument
    onTypeParameter1(this.getWithType());
    // :: error: argument
    onTypeParameter1(this.getWithString());
    // :: error: argument
    onTypeParameter1(getWithType());
    // :: error: argument
    onTypeParameter1(getWithString());
    // :: error: argument
    onTypeParameter1(param.getWithType());
    // :: error: argument
    onTypeParameter1(param.getWithString());
  }

  public void testOnParameter(Foo<String, String> param) {
    // :: error: argument
    onParameter(this.getWithType());
    // :: error: argument
    onParameter(this.getWithString());
    // :: error: argument
    onParameter(getWithType());
    // :: error: argument
    onParameter(getWithString());
    // :: error: argument
    onParameter(param.getWithType());
    // :: error: argument
    onParameter(param.getWithString());
  }

  public @RUntainted Foo<String, String> testOnParamReturn() {
    // :: error: return
    return withString;
  }

  public Foo<@RUntainted String, String> testOnTypeParamReturn() {
    // :: error: return
    return withString;
  }

  void mapEntryTest(Map.Entry<String, List<String>> entry) {
    // :: error: assignment
    List<@RUntainted String> values = entry.getValue();
  }
}

class TypeMapSelectTest {
  void testOnTypeArgumentMapSelection() {
    C<String, String, String> c = new C<String, String, String>();
    // :: error: assignment
    A<String, @RUntainted String> a = c.getB().getA();
  }

  class A<I, H> {}

  class B<M, N, P> {
    A<N, M> a;

    A<N, M> getA() {
      return a;
    }
  }

  class C<R, Q, L> {
    B<Q, L, R> b;

    B<Q, L, R> getB() {
      return b;
    }
  }
}

class ParameterizedReceiverDetection {

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
    // :: error: assignment
    @RUntainted String s0 = foo.getMap().keySet().iterator().next();
    // :: error: assignment
    @RUntainted String s1 = bar.getMap().keySet().iterator().next();
  }

  public void testcomplex(Foo1<List<String>, String> foo) {
    // :: error: assignment
    Bar1<String, List<@RUntainted List<@RUntainted String>>> s = foo.getBar();
  }

  class Foo1<K, M> {

    Bar1<M, List<K>> getBar() {
      return null;
    }
  }

  class Bar1<T, U> {
    T t;
    U u;

    T getT() {
      return t;
    }

    U getU() {
      return u;
    }
  }
}

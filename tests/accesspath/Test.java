import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

  HashMap<String, String> foo;
  Inner inner;
  Baz<String> baz;

  void bar() {
    // :: error: assignment
    @RUntainted String untaintedVar = foo.keySet().iterator().next();
    // :: error: assignment
    untaintedVar = inner.bar.keySet().iterator().next();
    // :: error: assignment
    untaintedVar = inner.test.foo.keySet().iterator().next();
    // :: error: assignment
    untaintedVar = baz.foo.getE();
    // :: error: assignment
    untaintedVar = baz.foo.getString();
    // :: error: assignment
    untaintedVar = baz.innerBaz.b.innerBaz.getE();
    // :: error: assignment
    untaintedVar = baz.innerBaz.getOther().getInnerBaz().getE();
  }

  class Inner {

    HashMap<String, String> bar;
    Test test;
  }

  class Baz<E> {
    Foo<E> foo;
    InnerBaz<String> innerBaz;

    class InnerBaz<R> {
      Baz<E> b;

      E getE() {
        return null;
      }

      InnerBaz<R> getInnerBaz() {
        // :: error: assignment
        @RUntainted E e = getE();
        return this;
      }

      InnerBaz<R> getOther() {
        return this;
      }
    }
  }
}

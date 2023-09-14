package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo {

  //  @RUntainted String s = "";
  //
  //  //  void run() {
  //  //    Map<String, String> map = new HashMap<>();
  //  //    // :: error: enhancedfor
  //  //    for (Map.Entry<@RUntainted String, @RUntainted String> entry : map.entrySet()) {
  //  //      s = entry.getKey();
  //  //    }
  //  //  }
  //
  void run() {
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

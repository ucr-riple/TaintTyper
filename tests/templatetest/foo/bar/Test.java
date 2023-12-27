package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {

  // This exposes a bug in CF
  //    private <T> T findFirst(final Function<? extends X, ? extends T> mapper) {
  //        return null;
  //    }
  //    public @RUntainted String test() {
  //        return findFirst(X::name);
  //    }
  //
  //    class X {
  //        public String name() {
  //            return "";
  //        }
  //    }
  //

  public String recurseMultipleTimes(String value, String valu2, String value3) {
    if (value instanceof String) {
      return recurseMultipleTimes(getPoly1(value), valu2, value3) + value;
    }
    if (valu2 instanceof String) {
      return recurseMultipleTimes(value, getPoly2(valu2), value3) + valu2;
    }
    if (value3 instanceof String) {
      return recurseMultipleTimes(value, valu2, getPoly3(value3)) + value3;
    }
    return recurseMultipleTimes(value, valu2, value3);
  }

  public String getPoly1(String value) {
    return value;
  }

  public String getPoly2(String value) {
    return value;
  }

  public String getPoly3(String value) {
    return value;
  }

  public void testMultiStatementRecursion() {
    // :: error: assignment
    @RUntainted String ans = recurseMultipleTimes("foo", "bar", "baz");
  }
}

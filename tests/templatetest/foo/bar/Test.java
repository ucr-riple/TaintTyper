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
}

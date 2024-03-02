package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo {

  void sink(@RUntainted Object o) {}

  @RUntainted
  ExpectInetrfaceTestForLambda testLambdasAreUntaintedUsedArguments() {
    return new ExpectInetrfaceTestForLambda(
        o -> {
          System.out.println(o);
        });
  }

  void testSerializationOfParameterForLambda() {
    new ExpectInetrfaceTestForLambda(
        o -> {
          // :: error: argument
          sink(o);
        });
  }

  class ExpectInetrfaceTestForLambda {
    ExpectInetrfaceTestForLambda(IBar i) {}
  }

  interface IBar {
    void m(Object o);
  }
}

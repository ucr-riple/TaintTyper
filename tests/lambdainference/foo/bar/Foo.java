package foo.bar;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;
import org.apache.commons.text.lookup.StringLookup;

public class Foo {
  String sink(@RUntainted Object o) {
    return null;
  }

  @RUntainted
  ExpectInetrfaceTestForLambda testLambdasAreUntaintedUsedArguments() {
    return new ExpectInetrfaceTestForLambda(
        o -> {
          System.out.println(o);
        });
  }

  void testParametersInLambdaExpressionForThirdPartyMethodAreUntainted() {
    expectStringLookup(key -> sink(key));
  }

  void expectStringLookup(StringLookup lookup) {}

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
}

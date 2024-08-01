package foo.bar;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;

public class Foo {

  public @RUntainted Object getUntainted() {
    return null;
  }

  public void getUntainted(Object pFoo) {
    return;
  }

  public List<@RUntainted String> untaintedStringListSuper() {
    return null;
  }

  public List<String> untaintedStringListChild() {
    return null;
  }

  public @RUntainted List<@RUntainted String> mix() {
    return null;
  }

  @RPolyTainted
  Object polyDiff(@RPolyTainted Object o) {
    return o;
  }
}

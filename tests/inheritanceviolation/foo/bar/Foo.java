package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo {

  public @RUntainted Object getUntainted() {
    return null;
  }

  public void getUntainted(Object pFoo) {
    return;
  }

  public List<@RUntainted String> untaintedStringListSuper(){
    return null;
  }

  public List<String> untaintedStringListChild(){
    return null;
  }
}

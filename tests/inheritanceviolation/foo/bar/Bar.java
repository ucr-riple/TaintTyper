package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Bar extends Foo {

//    // :: error: (override.return)
//    public Object getUntainted() {
//      return null;
//    }
//
//    // :: error: (override.param)
//    public void getUntainted(@RUntainted Object pBar) {
//      return;
//    }
//
//  // :: error: (override.return)
//  public List<String> untaintedStringListSuper() {
//    return null;
//  }

  // :: error: (override.return)
  public List<@RUntainted String> untaintedStringListChild() {
    return null;
  }

//  // :: error: (override.return)
//  public List<@RUntainted String> mix() {
//    return null;
//  }
}

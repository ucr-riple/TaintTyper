package com.test;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.HashMap;
import java.util.Map;

public class Foo {
  Map<String, String> onRerurn() {
    Map<String, @RUntainted String> ans = new HashMap<>();
    // :: error: return
    return ans;
  }

  void onAssigment() {
    Map<String, @RUntainted String> value = new HashMap<>();
    // :: error: assignment
    Map<String, String> var = value;
    // :: error: assignment
    var = value;
  }

  void onPassing(Map<String, @RUntainted String> p) {
    // :: error: argument
    bar(p);
  }

  void bar(Map<String, String> param) {}
}

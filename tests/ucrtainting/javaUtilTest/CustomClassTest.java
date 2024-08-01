package javaUtilTest;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;

class CustomClassTest<E> extends ArrayList<E> {
  void addStringLiteralToUntaintedClassUntaintedArg() {
    CustomClassTest<@RUntainted String> list = new CustomClassTest<>();
    list.add("string_literal");
    @RUntainted String s = list.get(0);
  }

  void addUntaintedStringToList(@RUntainted String s) {
    CustomClassTest<@RUntainted String> list = new CustomClassTest<>();
    list.add(s);
    @RUntainted String uStr = list.get(0);
  }

  void addTaintedStringToList(@RTainted String s) {
    CustomClassTest<@RUntainted String> list = new CustomClassTest<>();
    // :: error: (argument)
    list.add(s);
  }
}

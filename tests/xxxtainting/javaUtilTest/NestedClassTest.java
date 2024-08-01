package javaUtilTest;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;

class NestedClassTest {
  public void addUntaintedListOfUntaintedStringTo2DUntaintedList() {
    @RUntainted List<@RUntainted List<@RUntainted String>> lists = new ArrayList<>();
    @RUntainted List<@RUntainted String> list = new ArrayList<>();
    list.add("string_literal");
    lists.add(list);
  }

  public void addListOfUnTaintedStringTo2DUntaintedList() {
    @RUntainted List<@RUntainted List<@RUntainted String>> lists = new ArrayList<>();
    List<@RUntainted String> list = new ArrayList<>();
    list.add("string_literal");
    lists.add(list);
  }
}

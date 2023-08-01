package javaUtilTest;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

class UtilOperationsTest {

  public void checkWithTaintedListTaintedStrings(@RTainted String s) {
    @RTainted List<@RTainted String> taintedList = new ArrayList<@RTainted String>();
    taintedList.add(s);
    // :: error: (argument)
    expectedUntaintedListUntaintedString(taintedList);

    expectedTaintedListTaintedString(taintedList);
  }

  public void checkWithUntaintedListTaintedString(@RTainted String s) {
    @RUntainted List<String> unTaintedList = new ArrayList<String>();
    unTaintedList.add(s);
    // :: error: (argument)
    expectedUntaintedListUntaintedString(unTaintedList);
  }

  public void checkWithUntaintedListUnTaintedString(@RUntainted String s) {
    @RUntainted
    List<@RUntainted String> unTaintedListUntaintedString = new ArrayList<@RUntainted String>();
    unTaintedListUntaintedString.add(s);
    expectedUntaintedListUntaintedString(unTaintedListUntaintedString);
  }

  public void checkWithListUntaintedString(@RUntainted String s) {
    List<@RUntainted String> listUntaintedString = new ArrayList<@RUntainted String>();
    listUntaintedString.add(s);
    expectedListUntaintedString(listUntaintedString);
  }

  public void checkWithListUntaintedStringContainingTaintedString(@RTainted String s) {
    List<String> listUntaintedString = new ArrayList<String>();
    listUntaintedString.add(s);
    // :: error: (argument)
    expectedListUntaintedString(listUntaintedString);
  }

  public void expectedUntaintedList(@RUntainted List<String> list) {
    // DO Nothing
  }

  public void expectedUntaintedListUntaintedString(@RUntainted List<@RUntainted String> list) {
    // DO Nothing
  }

  public void expectedTaintedListTaintedString(@RTainted List<@RTainted String> list) {
    // DO Nothing
  }

  public void expectedListUntaintedString(List<@RUntainted String> list) {
    // DO Nothing
  }

  public void addTaintedStringToListOfUntaintedString(@RTainted String s) {
    List<@RUntainted String> list = new ArrayList<@RUntainted String>();
    // :: error: (argument)
    list.add(s);
  }

  public void addStringLiteralToListOfUntaintedString() {
    List<@RUntainted String> list = new ArrayList<@RUntainted String>();
    list.add("string_literal");
  }

  public void addTaintedStringToListOfString(@RTainted String s) {
    List<String> list = new ArrayList<String>();
    list.add(s);
  }

  public void addStringLiteralToListOfString(@RTainted String s) {
    List<String> list = new ArrayList<String>();
    list.add("string_literal");
  }

  public void getFromUntaintedListOfUntaintedStringLiterals() {
    @RUntainted List<@RUntainted String> list = new ArrayList<@RUntainted String>();
    list.add("string_literal1");
    list.add("string_literal2");
    @RUntainted String s = list.get(0);
  }

  public void getFromUntaintedListOfStringLiterals() {
    @RUntainted List<String> list = new ArrayList<String>();
    list.add("string_literal1");
    list.add("string_literal2");
    // :: error: (assignment)
    @RUntainted String s = list.get(0);
  }

  void getListTestPassingStringLiteralCheckForPolyTaintedReturn() {
    List<String> uList = getListPolyTainted("string_literal");
  }

  void getListTestPassingTaintedStringCheckForPolyTaintedReturn(@RTainted String s) {
    // :: error: (assignment)
    @RUntainted List<String> uList = getListPolyTainted(s);
  }

  List<String> getListPolyTainted(String s) {
    List<String> list = new ArrayList<String>();
    list.add(s);
    return list;
  }
}

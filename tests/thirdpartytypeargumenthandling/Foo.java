package test;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;
import javax.servlet.http.*;

public class Foo {

  public void errorOnReadingFromTaintedList() {
    List<String> taintedList = new ArrayList<>();
    // :: error: assignment
    @RUntainted String s = taintedList.get(0);
  }

  public void notErrorOnListIterator() {
    @RUntainted List<@RUntainted String> list = new ArrayList<>();
    // Should not be error here.
    Iterator<@RUntainted String> iterator = list.iterator();
  }
}

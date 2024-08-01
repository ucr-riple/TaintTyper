package javaUtilTest;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
import java.util.*;

class ArrayOperationsTest {
  public void accessUntaintedArrayAtIndex() {
    @RUntainted String[] arr = new String[] {"test1", "test2", "test3"};
    @RUntainted String uStr = arr[0];
  }

  public void accessArrayOfStringLiteralsAtIndex() {
    String[] arr = new String[] {"test1", "test2", "test3"};
    // :: error: (assignment)
    @RUntainted String uStr = arr[0];
  }

  public void assignUntaintedArrayWithTaintedDataAtIndex(@RTainted String s) {
    @RUntainted String[] arr = new String[] {"test1", "test2"};
    // :: error: (assignment)
    arr[1] = s;
  }
}

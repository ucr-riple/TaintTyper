package localTaint;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RUntainted;

public class LocatTaint {

  public void test() {
    int[] arr = getUntaintedArr();
    // :: error: argument
    sink(arr[0]);
  }

  public void sink(@RUntainted int val) {}

  public @RUntainted int[] getUntaintedArr() {
    return new int[] {1, 2, 3};
  }
}

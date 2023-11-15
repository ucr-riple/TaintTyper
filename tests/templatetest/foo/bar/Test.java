package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.List;

public class Test {

  List<String> devices = new ArrayList<>();

  public void test() {
    // :: error: assignment
    @RUntainted String s = Download.chooseBoard(devices);
  }
}

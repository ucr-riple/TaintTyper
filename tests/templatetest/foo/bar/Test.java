package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.ArrayList;
import java.util.List;

public class Test {

  List<@RUntainted String> devices = new ArrayList<>();
  // :: error: assignment
  java.lang.@RUntainted String selection = Download.chooseBoard(devices);
}

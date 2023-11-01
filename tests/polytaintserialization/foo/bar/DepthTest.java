package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;

public class DepthTest {

  //  String foo1(String param) {
  //    String s = param;
  //    String ans = foo2(s);
  //    return ans;
  //  }
  //
  //  String foo2(String param) {
  //    String s = param;
  //    String ans = foo3(s);
  //    return ans;
  //  }
  //
  //  String foo3(String param) {
  //    String s = param;
  //    String ans = foo4(s);
  //    return ans;
  //  }
  //
  //  String foo4(String param) {
  //    String s = param;
  //    String ans = foo5(s);
  //    return ans;
  //  }
  //
  //  String foo5(String param) {
  //    String s = param;
  //    String ans = foo6(s);
  //    return ans;
  //  }
  //
  //  String foo6(String param) {
  //    String s = param;
  //    String ans = foo7(s);
  //    return ans;
  //  }
  //
  //  String foo7(String param) {
  //    String s = param;
  //    String ans = foo8(s);
  //    return ans;
  //  }
  //
  //  String foo8(String param) {
  //    String s = param;
  //    String ans = foo9(s);
  //    return ans;
  //  }
  //
  //  String foo9(String param) {
  //    String s = param;
  //    String ans = foo10(s);
  //    return ans;
  //  }
  //
  //  String foo10(String param) {
  //    String s = param;
  //    String ans = s;
  //    return ans;
  //  }
  //
  //  String bar1(String param) {
  //    String s = param;
  //    String ans = bar2(s);
  //    return ans;
  //  }
  //
  //  String bar2(String param) {
  //    String s = param;
  //    String ans = bar3(s);
  //    return ans;
  //  }
  //
  //  String bar3(String param) {
  //    String s = param;
  //    String ans = bar4(s);
  //    return ans;
  //  }
  //
  //  String bar4(String param) {
  //    String s = param;
  //    return s;
  //  }
  //
  //  public void test(String param) {
  //    // :: error: assignment
  //    @RUntainted String outOfBound = foo1(param);
  //    // :: error: assignment
  //    @RUntainted String inBound = bar1(param);
  //  }
}

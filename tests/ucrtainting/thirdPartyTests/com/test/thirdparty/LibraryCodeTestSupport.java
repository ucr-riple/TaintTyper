package thirdPartyTests.com.test.thirdparty;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryCodeTestSupport {

  public static final LibraryCodeTestSupport singleTon = new LibraryCodeTestSupport("");
  private String str;

  public LibraryCodeTestSupport(String str) {
    this.str = str;
  }

  public String getVal() {
    return this.str;
  }

  public <T> T getVal(Class<T> c, Object o) {
    return c.cast(o);
  }

  public String getVal(Class<?> c, Object o, boolean flag) {
    return o.toString();
  }

  public void setVal(@RTainted String y) {
    this.str = y;
  }

  public List<String> list(String s) {
    String[] listStr = s.split(",");
    return Arrays.asList(listStr);
  }
}

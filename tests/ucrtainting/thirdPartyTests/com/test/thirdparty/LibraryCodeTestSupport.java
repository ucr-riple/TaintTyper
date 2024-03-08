package thirdPartyTests.com.test.thirdparty;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;

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

  public void setVal(@RTainted String y) {
    this.str = y;
  }
}

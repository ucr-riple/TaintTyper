package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

public class Foo {

  protected static final String MIME_HTML_TEXT = "text/html";
  public List<String> param;

  protected void writeLoginPageLink(HttpServletResponse resp) {
    resp.setContentType(MIME_HTML_TEXT);
    // :: error: assignment
    @RUntainted String s = doInSystemTransaction(param);
  }

  protected <T> T doInSystemTransaction(List<T> list) {
    return null;
  }
}

//  private static final Pattern PATTERN_CRLF = Pattern.compile("(\\r|\\n)");

//  private @RUntainted String[] processProperties;
//
//  public void setProcessProperties(Map<String, String> processProperties) {
//    ArrayList<@RUntainted String> processPropList = new ArrayList<>(processProperties.size());
//    boolean hasPath = false;
//    String systemPath = System.getenv("PATH");
//    // :: error: enhancedfor
//    for (Map.Entry<@RUntainted String, @RUntainted String> entry : processProperties.entrySet()) {
//      @RUntainted String key = entry.getKey();
//      @RUntainted String value = entry.getValue();
//      if (key == null) {
//        continue;
//      }
//      if (value == null) {
//        value = "";
//      }
//      key = key.trim();
//      value = value.trim();
//      if (key.equals("PATH")) {
//        if (systemPath != null && systemPath.length() > 0) {
//          processPropList.add(key + "=" + value + File.pathSeparator + systemPath);
//        } else {
//          processPropList.add(key + "=" + value);
//        }
//        hasPath = true;
//      } else {
//        processPropList.add(key + "=" + value);
//      }
//    }
//    if (!hasPath && systemPath != null && systemPath.length() > 0) {
//      processPropList.add("PATH=" + systemPath);
//    }
//    this.processProperties = processPropList.toArray(new String[processPropList.size()]);
//  }
//
//  private @RUntainted String sanitize(String redirectUrl)
//  {
//    if (redirectUrl != null)
//    {
//      return PATTERN_CRLF.matcher(redirectUrl).replaceAll("");
//    }
//
//    return null;
//  }
// }

// public class Foo<E extends Foo<E>> {
//
//  public void test(@RUntainted Bar bar) {
//    // :: error: type.argument
//    get(bar);
//  }
//
//  public void test1(String op1) {
//    @RUntainted String s = "hello";
//    // :: error: compound.assignment
//    s += op1;
//  }
//
//  static <E extends Foo<E>> E get(E e) {
//    return null;
//  }
//
//  private Map<@RUntainted String, @RUntainted String> headers;
//
//  public Map<String, String> getHeaders() {
//    // :: error: return
//    return headers;
//  }
// }
//
// class Bar extends Foo<Bar> {}

// Uncomment when new release of Checker Framework is available
// interface ListPage<E> extends List<E> {}
//
// class ArrayListPage<E> extends ArrayList<E> {
//  public ArrayListPage(final List<E> list) {
//    super(list != null ? list : Collections.emptyList());
//  }
// }

package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

public class Foo {

  protected static final String MIME_HTML_TEXT = "text/html";
  public List<String> param;
  private static final Pattern PATTERN_CRLF = Pattern.compile("(\\r|\\n)");
  private @RUntainted String[] processProperties;

  protected void writeLoginPageLink(HttpServletResponse resp) {
    resp.setContentType(MIME_HTML_TEXT);
    // :: error: assignment
    @RUntainted String s = doInSystemTransaction(param);
  }

  public void test1(String op1) {
    @RUntainted String s = "hello";
    // :: error: compound.assignment
    s += op1;
  }

  protected <T> T doInSystemTransaction(List<T> list) {
    return null;
  }

  public void setProcessProperties(Map<String, String> processProperties) {
    ArrayList<@RUntainted String> processPropList = new ArrayList<>(processProperties.size());
    boolean hasPath = false;
    String systemPath = System.getenv("PATH");
    // :: error: enhancedfor
    for (Map.Entry<@RUntainted String, @RUntainted String> entry : processProperties.entrySet()) {
      @RUntainted String key = entry.getKey();
      @RUntainted String value = entry.getValue();
      if (key == null) {
        continue;
      }
      if (value == null) {
        value = "";
      }
      key = key.trim();
      value = value.trim();
      if (key.equals("PATH")) {
        if (systemPath != null && systemPath.length() > 0) {
          processPropList.add(key + "=" + value + File.pathSeparator + systemPath);
        } else {
          processPropList.add(key + "=" + value);
        }
        hasPath = true;
      } else {
        processPropList.add(key + "=" + value);
      }
    }
    if (!hasPath && systemPath != null && systemPath.length() > 0) {
      processPropList.add("PATH=" + systemPath);
    }
    this.processProperties = processPropList.toArray(new String[processPropList.size()]);
  }

  private @RUntainted String sanitize(String redirectUrl) {
    if (redirectUrl != null) {
      // :: error: return
      return PATTERN_CRLF.matcher(redirectUrl).replaceAll("");
    }
    return null;
  }
}

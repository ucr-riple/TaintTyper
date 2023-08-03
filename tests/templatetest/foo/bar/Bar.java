package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import javax.servlet.http.HttpServletResponse;

public class Bar extends Foo {

  @Override
  protected void writeLoginPageLink(HttpServletResponse resp) {
    resp.setContentType(MIME_HTML_TEXT);
  }
}

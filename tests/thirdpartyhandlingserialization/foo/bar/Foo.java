package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;

class Foo {

  String path;

  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("text/html;charset=UTF-8");

    // :: error: (assignment)
    @RUntainted String param = request.getHeader("BenchmarkTest00175");
    // :: error: (assignment)
    @RUntainted File file = new File(path);
  }
}

package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;

class Foo {

  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("text/html;charset=UTF-8");

    // :: error: (assignment)
    @RUntainted String param = request.getHeader("BenchmarkTest00175");
  }
}

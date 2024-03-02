package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Base64;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

class Foo {

  enum MyEnum {
    A,
    B,
    C
  }

  String path;

  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("text/html;charset=UTF-8");
    // :: error: (assignment)
    @RUntainted String param = request.getHeader("BenchmarkTest00175");
    // :: error: (assignment)
    // :: error: (argument)
    @RUntainted File file = new File(path);
    try {
      @RUntainted Path tmpFile = Files.createTempFile("jar_cache", null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setContentType("text/html;charset=UTF-8");
    javax.servlet.http.Cookie userCookie =
        new javax.servlet.http.Cookie("BenchmarkTest00093", "ls");
    userCookie.setMaxAge(60 * 3); // Store cookie for 3 minutes
    userCookie.setSecure(true);
    // :: error: argument
    userCookie.setPath(request.getRequestURI());
    // :: error: argument
    userCookie.setDomain(new java.net.URL(request.getRequestURL().toString()).getHost());
    response.addCookie(userCookie);
    javax.servlet.RequestDispatcher rd =
        request.getRequestDispatcher("/cmdi-00/BenchmarkTest00093.html");
    rd.include(request, response);
  }

  public Stream<@RUntainted String> testOnStreamLambda(List<String> s) {
    // :: error: return
    return s.stream().filter(x -> x.length() > 0);
  }

  public void test() {
    @RUntainted String nonceValue = Base64.getUrlEncoder().encodeToString(getRandomBytes());
  }

  private @RUntainted byte[] getRandomBytes() {
    return new byte[0];
  }

  public void testCheckTypeForArgumentsBeforeCallingFixVisitor(
      HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("text/html;charset=UTF-8");

    // :: error: (assignment)
    @RUntainted String param = request.getHeader(MyEnum.A.toString());
  }

  public void testSkipOnTypeArgUntaintedFromThirdParty(@RUntainted MBeanServer mBeanServer) {
    Set<@RUntainted ObjectName> res = mBeanServer.queryNames(null, null);
  }

  class WebDAVServlet extends HttpServlet {
    public void testContextCreation() {
      @RUntainted
      WebApplicationContext context =
          WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    }
  }

  Point loc;

  public void testErrorForThirdPartyFieldSelectionError(Point p) {
    // :: error: assignment
    @RUntainted int i = p.x;
  }

  public void testErrorForThirdPartyFieldSelectionFix(@RUntainted Point p) {
    @RUntainted int i = p.x;
  }

  public @RUntainted String testDeepUntaintedFoFieldAccess() {
    return convert(String.class);
  }

  private <T> T convert(final Class<T> cls) {
    return null;
  }
}

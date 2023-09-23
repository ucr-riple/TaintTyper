package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;
import com.opensymphony.xwork2.util.TextParseUtil;
import com.opensymphony.xwork2.ActionInvocation;

public class Test {

  @RPolyTainted
  String foo(@RPolyTainted String s0, String s1) {
    return s0;
  }

  protected @RUntainted String contentType = "text/plain";

  public void simple(String param) {
    @RUntainted String s = foo("foo", param);
    // :: error: assignment
    @RUntainted String s2 = foo(param, "bar");
    List<String> list = new ArrayList<>();
    // :: error: assignment
    @RUntainted String s3 = foo(list.get(0), "bar");
  }

  public @RPolyTainted String thirdParty(@RPolyTainted int index, @RPolyTainted String s) {
    return s.substring(index);
  }

  public void test(HttpServletResponse oResponse, ActionInvocation invocation) {
    oResponse.setContentType(conditionalParse(contentType, invocation));
  }

  protected @RPolyTainted String conditionalParse(
      @RPolyTainted String param, ActionInvocation invocation) {
    return param;
  }

  public String inferPoly(String param) {
    String s = param;
    return s;
  }

  public void test() {
    // :: error: assignment
    @RUntainted String s = inferPoly("foo");
  }

  public void inferUntainted(String param) {
    // :: error: assignment
    @RUntainted String s = conditionalParse(param, null);
  }

  public String inferPolyWithTypeParamAnnotation(List<String> param) {
    String s = param.get(0);
    return s;
  }

  public void inferPolyWithTypeParamAnnotationTest(List<@RUntainted String> p1, List<String> p2) {
    //    inferPolyWithTypeParamAnnotation(p1);
    // :: error: assignment
    @RUntainted String s2 = inferPolyWithTypeParamAnnotation(p2);
  }

  public static String getFolderPath(String resource) {
    return resource.substring(0, resource.lastIndexOf('/') + 1);
  }

  public static String getPathPart(String resource, int level) {
    resource = getFolderPath(resource);
    String result = null;
    int pos = 0, count = 0;
    if (level >= 0) {
      while ((count < level) && (pos > -1)) {
        count++;
        pos = resource.indexOf('/', pos + 1);
      }
    } else {
      pos = resource.length();
      while ((count > level) && (pos > -1)) {
        count--;
        pos = resource.lastIndexOf('/', pos - 1);
      }
    }
    if (pos > -1) {
      result = resource.substring(0, pos + 1);
    } else {
      result = (level < 0) ? "/" : resource;
    }
    return result;
  }

  public String simpleLoopLocalVariableAssigment(String param) {
    String l1 = param;
    String l2 = l1;
    l1 = l2;
    l2 = l1;
    return l2;
  }

  public void inferPolyForRecursiveLocalVariablesAssignment() {
    // :: error: assignment
    @RUntainted String s1 = getPathPart("foo/bar/baz", 1);
    // :: error: assignment
    @RUntainted String s2 = simpleLoopLocalVariableAssigment("foo/bar/baz");
  }

  public boolean parse;
  protected @RPolyTainted String conditionalParse2(@RPolyTainted String param, @RPolyTainted ActionInvocation invocation) {
    if (parse && param != null && invocation != null) {
      return TextParseUtil.translateVariables(
              param,
              invocation.getStack(),
              new EncodingParsedValueEvaluator());
    } else {
      return param;
    }
  }

  class EncodingParsedValueEvaluator implements TextParseUtil.ParsedValueEvaluator {
    @Override
    public Object evaluate(Object parsedValue) {
      return parsedValue;
    }
  }
}

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

class Foo {

  String field = "";
  Bar bar = new Bar();

  void bar(String x, @RUntainted String y, boolean b) {
    String localVar = x;
    final String finalLocalVar = x;
    // :: error: assignment
    @RUntainted String c = field;
    // :: error: assignment
    c = x;
    // :: error: assignment
    c = b ? x : y;
    // :: error: assignment
    c = x + y;
    // :: error: assignment
    c = b ? x + y : localVar;
    if (b) {
      // :: error: assignment
      c = bar.getField();
    } else {
      // :: error: assignment
      c = bar.field;
      // :: error: assignment
      c = x;
      // :: error: assignment
      c = localVar;
      // :: error: assignment
      c = bar.baz.field;
    }
    class LocalClass {
      @RUntainted String field;
      String f2;

      void foo(
          TypeArgument<
                  String,
                  String,
                  Map<String, String>,
                  HashMap<String, String>,
                  HashMap<HashMap<String, String>, Map<?, String>>>
              ta) {
        // :: error: assignment
        field = finalLocalVar;
        // :: error: assignment
        field = f2;
        String localVar = f2;
        List<String> argsList = new ArrayList<>();
        String[] argsArray = new String[10];
        class LocalInnerClass {

          // :: error: assignment
          @RUntainted String baz = localVar + argsList.get(0) + argsArray[0] + ta.getT();

          void run() {
            // :: error: assignment
            baz = ta.getT();
            // :: error: assignment
            baz = ta.getE();
            // :: error: assignment
            baz = ta.getJ().toString();
            // :: error: assignment
            baz = ta.getJ().keySet().iterator().next();
            // :: error: assignment
            baz = ta.getJ().values().iterator().next();
          }
        }
      }
    }
    // :: error: assignment
    c = bar.staticF;
    // :: error: argument
    requireMap(getTypeArgument().getJ());
  }

  @RUntainted
  Object returnUntainted() {
    Object o = new Object();
    // :: error: argument
    requireUntainted(o);
    // :: error: return
    return o;
  }

  void requireUntainted(@RUntainted Object param) {}

  public void inheritParam(Object param) {}

  public @RUntainted Object inheritReturn() {
    return null;
  }

  public TypeArgument<
          String,
          String,
          Map<String, String>,
          HashMap<String, String>,
          HashMap<HashMap<String, String>, Map<?, String>>>
      getTypeArgument() {
    return null;
  }

  public void requireMap(@RUntainted Map<?, ?> map) {}
}

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

class Foo {
  @RUntainted String field;
  Other other = new Other();
  boolean b;

  void foo(
      TypeArgument<
              String,
              String,
              Map<String, String>,
              HashMap<String, String>,
              HashMap<HashMap<String, String>, Map<?, String>>>
          ta) {

    // :: error: assignment
    field = ta.getT();
    // :: error: assignment
    field = ta.getE();
    // :: error: assignment
    field = ta.getJ().toString();
    // :: error: assignment
    field = ta.getJ().keySet().iterator().next();
    // :: error: assignment
    field = ta.getJ().values().iterator().next();
    // :: error: argument
    requireMap(getTypeArgument().getJ());
    // :: error: assignment
    field = other.getO().getT();
    // :: error: assignment
    field = other.inner.innerField.getT();

    MapTypeArgument<String, String, HashMap<String, String>> mapTypeArgument =
        new MapTypeArgument<>();
    @RUntainted Map<?, ?> map;
    // :: error: assignment
    map = mapTypeArgument.c;
    // :: error: assignment
    map = other.inner.innerField.getJ();
    // :: error: assignment
    map = b ? other.inner.innerField.getJ() : mapTypeArgument.c;
    // :: error: assignment
    map = b ? other.inner.innerField.getJ() : mapTypeArgument.getC();
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

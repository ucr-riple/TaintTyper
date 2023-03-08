import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo<E, D> {

  private final Foo<String, String> withString = null;
  private final Foo<E, D> withType = null;

  public Foo<E, D> getWithType() {
    return withType;
  }

  public Foo<String, String> getWithString() {
    return withString;
  }

  public void requireOnTypeParameter1(Foo<@RUntainted ?, ?> p) {}

  public void requireOnTypeParameter2(Foo<?, @RUntainted ?> p) {}

  public void requireOnTypeParameterAll(Foo<@RUntainted ?, @RUntainted ?> p) {}

  public void requireOnParameter(@RUntainted Foo<?, ?> p) {}

  public void coreTest() {
    // :: error: argument
    requireOnTypeParameter1(getWithType());
    // :: error: argument
    requireOnParameter(getWithType());
  }

  public void paramIndexDetection() {
    // :: error: argument
    requireOnTypeParameter1(getWithType());
    // :: error: argument
    requireOnTypeParameter2(getWithType());
    // :: error: argument
    requireOnTypeParameterAll(getWithType());
  }

  public void testOnTypeParameter(Foo<String, String> param) {
    // :: error: argument
    requireOnTypeParameter1(this.getWithType());
    // :: error: argument
    requireOnTypeParameter1(this.getWithString());
    // :: error: argument
    requireOnTypeParameter1(getWithType());
    // :: error: argument
    requireOnTypeParameter1(getWithString());
    // :: error: argument
    requireOnTypeParameter1(param.getWithType());
    // :: error: argument
    requireOnTypeParameter1(param.getWithString());
  }

  public void testOnParameter(Foo<String, String> param) {
    // :: error: argument
    requireOnParameter(this.getWithType());
    // :: error: argument
    requireOnParameter(this.getWithString());
    // :: error: argument
    requireOnParameter(getWithType());
    // :: error: argument
    requireOnParameter(getWithString());
    // :: error: argument
    requireOnParameter(param.getWithType());
    // :: error: argument
    requireOnParameter(param.getWithString());
  }

  public @RUntainted Foo<String, String> testOnParamReturn() {
    // :: error: return
    return withString;
  }

  public Foo<@RUntainted String, String> testOnTypeParamReturn() {
    // :: error: return
    return withString;
  }
}

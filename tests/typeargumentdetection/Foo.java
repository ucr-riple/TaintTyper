import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo<E> {

  private final Foo<String> withString = null;
  private final Foo<E> withType = null;

  public Foo<E> getWithType() {
    return withType;
  }

  public Foo<String> getWithString() {
    return withString;
  }

  public void requireOnTypeParameter(Foo<@RUntainted ?> p) {}

  public void requireOnParameter(@RUntainted Foo<?> p) {}

  public void testOnTypeParameter(Foo<String> param) {
    // :: error: argument
    requireOnTypeParameter(this.getWithType());
    // :: error: argument
    requireOnTypeParameter(this.getWithString());
    // :: error: argument
    requireOnTypeParameter(getWithType());
    // :: error: argument
    requireOnTypeParameter(getWithString());
    // :: error: argument
    requireOnTypeParameter(param.getWithType());
    // :: error: argument
    requireOnTypeParameter(param.getWithString());
  }

  public void testOnParameter(Foo<String> param) {
    // :: error: argument
    requireOnTypeParameter(this.getWithType());
    // :: error: argument
    requireOnTypeParameter(this.getWithString());
    // :: error: argument
    requireOnTypeParameter(getWithType());
    // :: error: argument
    requireOnTypeParameter(getWithString());
    // :: error: argument
    requireOnTypeParameter(param.getWithType());
    // :: error: argument
    requireOnTypeParameter(param.getWithString());
  }

  public @RUntainted Foo<String> testOnParamReturn() {
    // :: error: return
    return withString;
  }

  public Foo<@RUntainted String> testOnTypeParamReturn() {
    // :: error: return
    return withString;
  }
}

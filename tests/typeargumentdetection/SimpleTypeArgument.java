import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class SimpleTypeArgument<E> {

  private final SimpleTypeArgument<String> withString = null;
  private final SimpleTypeArgument<E> withType = null;

  SimpleTypeArgument<String> get() {
    return null;
  }

  public SimpleTypeArgument<E> getWithType() {
    return withType;
  }

  public SimpleTypeArgument<String> getWithString() {
    return withString;
  }

  public void require(SimpleTypeArgument<@RUntainted ?> s) {}

  public void test(SimpleTypeArgument<String> param) {
    // :: error: argument
    require(this.getWithType());
    // :: error: argument
    require(this.getWithString());
    // :: error: argument
    require(getWithType());
    // :: error: argument
    require(getWithString());
    // :: error: argument
    require(param.getWithType());
    // :: error: argument
    require(param.getWithString());
  }
}

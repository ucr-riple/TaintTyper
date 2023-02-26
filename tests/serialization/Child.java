import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Child extends Foo {

  @Override
  // :: error: (override.param)
  public void inheritParam(@RUntainted Object paramInChild) {}

  @Override
  // :: error: (override.return)
  public Object inheritReturn() {
    return new Object();
  }
}

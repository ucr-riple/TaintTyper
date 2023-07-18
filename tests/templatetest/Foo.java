import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.*;
import java.util.*;

public class Foo<E extends Foo<E>> {

  public void test(@RUntainted Bar bar) {
    // :: error: type.argument
    get(bar);
  }

  static <E extends Foo<E>> E get(E e) {
    return null;
  }
}

class Bar extends Foo<Bar> {}

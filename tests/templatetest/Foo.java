import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.*;
import java.util.*;

public class Foo {

  public void test(@RUntainted Path path) throws Exception {
    // :: error: type.argument
    EnumSet.of(StandardOpenOption.READ);
  }
}

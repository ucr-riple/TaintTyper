package stubMalfunc;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import java.io.File;

public class Test {
  public static File toFile(@RTainted String value) {
    if (value instanceof String) {
      // :: error: argument
      return new File(value);
    }
    return new File("./");
  }
}

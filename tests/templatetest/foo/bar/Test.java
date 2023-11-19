package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.File;
import org.apache.commons.cli.Option;

public class Test {

  public void test(@RUntainted Option opt) {
    final @RUntainted File fileB = new File(opt.getValues()[1]);
  }
}

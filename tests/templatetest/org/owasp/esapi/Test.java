package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import org.owasp.esapi.Encoder;

public class Test {

  Encoder encoder;

  public void exec() {
    sink(encoder.encodeForHTML(""));
  }

  void sink(@RUntainted Object o) {}
}

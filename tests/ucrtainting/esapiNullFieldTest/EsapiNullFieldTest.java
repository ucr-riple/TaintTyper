package esapiNullFieldTest;

import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;

public class EsapiNullFieldTest {
  public String fieldData = null;

  public void setter(String s) {
    this.fieldData = s;
  }

  public @RUntainted String source() {
    // :: error: return
    return this.fieldData;
  }

  public void sink(@RUntainted String data) {}
}

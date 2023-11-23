package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.Optional;
import org.apache.commons.digester3.CallMethodRule;
import org.apache.commons.digester3.Digester;
import org.xml.sax.Attributes;

public class Test {

  public void test(Digester digester) {
    digester.addRule(
        null,
        new CallMethodRule(null, 15, new Class[] {}) {
          @Override
          public void begin(String namespace, String name, Attributes attributes) throws Exception {
            getDigester().peekParams()[14] = Optional.empty();
          }
        });
  }
}

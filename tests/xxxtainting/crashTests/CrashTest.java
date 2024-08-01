package crashTests;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class CrashTest {
  private TestEnum http1 = TestEnum.HTTP_1;

  @SuppressWarnings("unchecked")
  private void classPackageNullTest() {
    if (this.getClass().equals(void.class)) {}
    List results = new ArrayList();

    results.toArray((Object[]) Array.newInstance(Object.class, results.size()));
  }
}

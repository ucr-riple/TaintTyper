package crashTests;

public class StringTest {
  private StringBuilderCrash crash = new StringBuilderCrash();

  @SuppressWarnings("unchecked")
  private void test() {
    StringBuilder builder =
        new StringBuilder((crash.isCrashing() ? "a" : "b"))
            .append(", max-age=")
            .append(crash.getSeconds());
  }
}

package esapiTest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

class ESAPICustomJavaLevel extends Level {
  public static final Level ALWAYS_LEVEL =
      new ESAPICustomJavaLevel("ALWAYS", Level.OFF.intValue() - 1);

  private ESAPICustomJavaLevel(String name, int value) {
    super(name, value);
  }
}

enum Wrapper {
  ALWAYS(ESAPICustomJavaLevel.ALWAYS_LEVEL);
  Level level;

  Wrapper(Level alwaysLevel) {
    this.level = alwaysLevel;
  }
}

public class Test {
  static {
    Map<Integer, Wrapper> levelLookup = new HashMap<>();
    levelLookup.put(0, Wrapper.ALWAYS);
  }

  Test() {}
}

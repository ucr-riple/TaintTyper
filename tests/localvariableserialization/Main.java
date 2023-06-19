import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

  public static final String PATH = "/";

  public void foo(String param) throws Exception {
    String[] testDirs = new String[0];
    Files.copy(Paths.get(param), Paths.get(param), StandardCopyOption.REPLACE_EXISTING);
  }

  public void bar() {
    try {
      // some code
    } catch (Exception e) {
      // :: error: assignment
      @RUntainted Exception dup = e;
    }
  }
}

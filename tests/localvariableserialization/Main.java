import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

  public static final String PATH = "/";

  public void foo() throws Exception {
    String[] testDirs = new String[0];
    Files.copy(
        // :: error: argument
        this.getClass().getResourceAsStream(testDirs[0] + "run.tcl"),
        Paths.get(Main.PATH + "run.tcl"),
        StandardCopyOption.REPLACE_EXISTING);
  }
}

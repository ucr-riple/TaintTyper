package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

  public static final String PATH = "/";
}

class Foo {

  public void run() throws Exception {
    Files.copy(
        this.getClass().getResourceAsStream(Main.PATH + "run.tcl"),
        Paths.get(Main.PATH + "run.tcl"),
        StandardCopyOption.REPLACE_EXISTING);
  }
}

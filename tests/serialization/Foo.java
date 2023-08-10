import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.jar.JarFile;

class Foo {

  String field = "";
  Bar bar = new Bar();

  static final String staticField = "";

  void bar(String x, @RUntainted String y, boolean b) {
    String localVar = x;
    final String finalLocalVar = x;
    // :: error: assignment
    @RUntainted String c = field;
    // :: error: assignment
    c = x;
    // :: error: assignment
    c = b ? x : y;
    // :: error: assignment
    c = x + y;
    // :: error: assignment
    c = b ? x + y : localVar;
    if (b) {
      // :: error: assignment
      c = bar.getField();
    } else {
      // :: error: assignment
      c = bar.field;
      // :: error: assignment
      c = x;
      // :: error: assignment
      c = localVar;
      // :: error: assignment
      c = bar.baz.field;
    }
    class LocalClass {
      @RUntainted String field;
      String f2;

      void foo() {
        // :: error: assignment
        field = finalLocalVar;
        // :: error: assignment
        field = f2;
        String localVar = f2;
        List<String> argsList = new ArrayList<>();
        String[] argsArray = new String[10];
        class LocalInnerClass {

          // :: error: assignment
          @RUntainted String baz = localVar + argsList.get(0) + argsArray[0];
        }
      }
    }
    // :: error: assignment
    c = bar.staticF;
  }

  void requireUntainted(@RUntainted Object param) {}

  public void inheritParam(Object param) {}

  public @RUntainted Object inheritReturn() {
    return null;
  }

  public void testAndOr(boolean op1, boolean op2) {
    // :: error: assignment
    @RUntainted boolean x = op1 && op2;
    // :: error: assignment
    x = op1 || op2;
  }

  public void testRunnableTempFile() {
    JarFile jarFile;
    try (final InputStream in = new BufferedInputStream(System.in)) {
      jarFile =
          AccessController.doPrivileged(
              new PrivilegedExceptionAction<JarFile>() {
                public JarFile run() throws IOException {
                  @RUntainted Path tmpFile = Files.createTempFile("jar_cache", null);
                  try {
                    Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                    JarFile jarFile =
                        new JarFile(
                            tmpFile.toFile(), true, JarFile.OPEN_READ | JarFile.OPEN_DELETE);
                    return jarFile;
                  } catch (Throwable thr) {
                    try {
                      Files.delete(tmpFile);
                    } catch (IOException ioe) {
                      thr.addSuppressed(ioe);
                    }
                    throw thr;
                  } finally {
                    in.close();
                  }
                }
              });
    } catch (Exception pae) {

    }
  }
}

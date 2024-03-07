package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JFileChooser;
import org.apache.commons.lang3.reflect.MethodUtils;

public class Foo {

  private Object field;
  private @RUntainted String[] processProperties;
  private static final String[] PROP_NAMES = {
    null, "user.home", "user.dir", "java.home", "java.io.tmpdir"
  };

  public void bar(Object param) {
    // :: error: array.initializer
    // :: error: assignment
    @RUntainted Object[] arr = {field, param};
  }

  public void arrayCopy(String[] existedProperties) {
    int epl = existedProperties.length;
    // :: error: assignment
    @RUntainted String[] newProperties = Arrays.copyOf(existedProperties, epl + 1);
  }

  public void toArrayTest(
      Bar<@RUntainted String, @RUntainted String, @RUntainted String> processPropList) {
    @RUntainted
    String[] processProperties = processPropList.toArray(new String[processPropList.size()]);
  }

  public void toArrayTestNoError(Bar<String, @RUntainted String, String> b) {
    @RUntainted String[] processProperties = b.toArray(new String[b.size()]);
  }

  public void toArrayFromMethodCall(Map<String, Bar<String, @RUntainted String, String>> b) {
    @RUntainted
    String[] processProperties = b.values().iterator().next().toArray(new String[b.size()]);
  }

  public void toArrayTestWithError(Map<String, Bar<@RUntainted String, String, String>> b) {
    @RUntainted
    // :: error: assignment
    String[] processProperties = b.values().iterator().next().toArray(new String[b.size()]);
  }

  public void toArrayTestWithErrorSimple(List<String> list) {
    @RUntainted
    // :: error: assignment
    String[] processProperties = list.toArray(new String[list.size()]);
  }

  public void toArraySimpleWithNoError(
      Map<@RUntainted String, @RUntainted String> processProperties) {
    ArrayList<@RUntainted String> processPropList =
        new ArrayList<@RUntainted String>(processProperties.size());
    this.processProperties = processPropList.toArray(new String[processPropList.size()]);
  }

  static class CustomList<E, N> extends ArrayList<N> {}

  static class Bar<P, Q, R> extends CustomList<R, Q> {}

  private int currentSplitSegmentIndex;
  private @RUntainted Path zipFile;

  private @RUntainted Path createNewSplitSegmentFile(final Integer zipSplitSegmentSuffixIndex)
      throws IOException {
    final int newZipSplitSegmentSuffixIndex =
        zipSplitSegmentSuffixIndex == null
            ? (currentSplitSegmentIndex + 2)
            : zipSplitSegmentSuffixIndex;
    final String baseName = FileNameUtils.getBaseName(zipFile);
    String extension = ".z";
    if (newZipSplitSegmentSuffixIndex <= 9) {
      extension += "0" + newZipSplitSegmentSuffixIndex;
    } else {
      extension += newZipSplitSegmentSuffixIndex;
    }

    final Path parent = zipFile.getParent();
    final String dir = Objects.nonNull(parent) ? parent.toAbsolutePath().toString() : ".";
    // :: error: (assignment)
    final @RUntainted Path newFile = zipFile.getFileSystem().getPath(dir, baseName + extension);
    return newFile;
  }

  static class FileNameUtils {
    public static @RPolyTainted String getBaseName(final @RPolyTainted Path path) {
      if (path == null) {
        return null;
      }
      final Path fileName = path.getFileName();
      return fileName.toString();
    }
  }

  public void vargarsMultipleArguments(String SQL_CLEAR_PROPERTY, String table, String keyColumn) {
    // :: error: assignment
    @RUntainted String b = String.format(SQL_CLEAR_PROPERTY, table, keyColumn);
  }

  private final JavaLogLevelHandlers javaLogLevelHandlers = JavaLogLevelHandlers.SEVERE;

  public void enhancedForLoopOnList(@RUntainted Object action) {
    List<@RUntainted Method> methods =
        new ArrayList<@RUntainted Method>(
            MethodUtils.getMethodsListWithAnnotation(
                action.getClass(), BeforeResult.class, true, true));
    for (@RUntainted Method m : methods) {
      try {
        MethodUtils.invokeMethod(action, true, m.getName());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @interface BeforeResult {
    int priority() default 10;
  }

  public void matchOnArgTypeArg() {
    // :: error: (argument)
    add(getArrayConnections(null, -1));
  }

  public static List<String> getArrayConnections(Object array, int id) {
    final java.util.ArrayList<java.lang.String> connections = new ArrayList<String>();
    connections.add("");
    return connections;
  }

  public Foo add(java.util.Collection<@RUntainted String> lines) {
    for (final java.lang.String line : lines) {}

    return this;
  }

  public static @RUntainted JFileChooser create() {
    // :: error: enhancedfor
    for (final java.lang.@RUntainted String prop : PROP_NAMES) {
      try {
        @RUntainted String dirname = null;
        if (prop == null) {
        } else {
          dirname = System.getProperty(prop);
        }
        if ("".equals(dirname)) {
          return new JFileChooser();
        } else {
          final java.io.File dir = new File(dirname);
          if (dir != null) {
            return new JFileChooser(dir);
          }
        }
      } catch (RuntimeException t) {
        throw new RuntimeException(t);
      }
    }
    return null;
  }

  public void inferParamToBeUntainted(String line) {
    // :: error: assignment
    final @RUntainted String[] property = inferPolyOnComponentType(line, false);
  }

  static String[] inferPolyOnComponentType(final String line, final boolean trimValue) {
    return line.split(" ");
  }

  public void inferForPolyComponent(String line) {
    // :: error: assignment
    final @RUntainted String[] property = inferedPolyOnComponentType(line, false);
  }

  static @RPolyTainted String[] inferedPolyOnComponentType(
      final @RPolyTainted String line, final boolean trimValue) {
    return line.split(" ");
  }

  void testVarArgsForArraysAsList(@RUntainted String a, String b) {
    // :: error: assignment
    List<@RUntainted String> list = Arrays.asList(a, b);
  }

  public void testReferenceTypeForVarArgsIsUntainted(Object... param) {
    convert(Object[].class, param);
  }

  private <T> T convert(final Class<T> cls, final T defValue) {
    return null;
  }

  String[] simpleArrayAccess;

  public void simpleArrayAccessTest() {
    // :: error: assignment
    @RUntainted String s = simpleArrayAccess[1];
  }
}

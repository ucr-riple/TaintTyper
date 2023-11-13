import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.reflect.MethodUtils;

public class Foo {

  private Object field;
  private @RUntainted String[] processProperties;

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
}

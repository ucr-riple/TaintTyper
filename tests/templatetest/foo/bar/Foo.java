package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Foo {

  private int currentSplitSegmentIndex;
  private @RUntainted Path zipFile;

  private @RUntainted Path createNewSplitSegmentFile(final Integer zipSplitSegmentSuffixIndex)
      throws IOException {
    final int newZipSplitSegmentSuffixIndex =
        zipSplitSegmentSuffixIndex == null
            ? (currentSplitSegmentIndex + 2)
            : zipSplitSegmentSuffixIndex;
    final String baseName = FileNameUtils.getBaseName(zipFile);
    @RUntainted String extension = ".z";
    if (newZipSplitSegmentSuffixIndex <= 9) {
      extension += "0" + newZipSplitSegmentSuffixIndex;
    } else {
      extension += newZipSplitSegmentSuffixIndex;
    }

    final Path parent = zipFile.getParent();
    final String dir = Objects.nonNull(parent) ? parent.toAbsolutePath().toString() : ".";
    // :: error: assignment
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
}

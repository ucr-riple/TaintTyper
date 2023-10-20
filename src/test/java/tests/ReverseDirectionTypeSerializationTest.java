package tests;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import java.io.File;
import java.util.List;
import org.junit.runners.Parameterized;
import tests.tools.SerializationTestHelper;

public class ReverseDirectionTypeSerializationTest extends SerializationTestHelper {
  public ReverseDirectionTypeSerializationTest(List<File> testFiles) {
    super(
        testFiles,
        UCRTaintingChecker.class,
        "ucrtainting",
        "-Anomsgtext",
        "-AannotatedPackages=com.test",
        "-AenableLibraryCheck",
        "-nowarn");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"reversedirection"};
  }
}

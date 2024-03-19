package tests;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import java.io.File;
import java.util.List;
import org.junit.runners.Parameterized.Parameters;
import tests.tools.SerializationTestHelper;

/**
 * Test runner for tests of the UCR Tainting Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/ucrtainting} folder. To add a new test case,
 * create a Java file in that directory. The file contains "// ::" comments to indicate expected
 * errors and warnings; see
 * https://github.com/typetools/checker-framework/blob/master/checker/tests/README .
 *
 * <p>This is a template test for writing new tests.
 */
public class NewTestTemplateTest extends SerializationTestHelper {
  public NewTestTemplateTest(List<File> testFiles) {
    super(
        testFiles,
        UCRTaintingChecker.class,
        "ucrtainting",
        "-nowarn",
        "-Xlint:removal",
        "-Anomsgtext",
        "-AannotatedPackages=foo.bar",
        "-AenableLibraryCheck");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"templatetest"};
  }
}

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Test runner for tests of the UCR Tainting Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/ucrtainting} folder. To add a new test case,
 * create a Java file in that directory. The file contains "// ::" comments to indicate expected
 * errors and warnings; see
 * https://github.com/typetools/checker-framework/blob/master/checker/tests/README .
 */
public class UCRTaintingLibraryTest extends CheckerFrameworkPerDirectoryTest {
  public UCRTaintingLibraryTest(List<File> testFiles) {
    super(
        testFiles,
        UCRTaintingChecker.class,
        "ucrtainting",
        "-Anomsgtext",
        "-AannotatedPackages=foo.bar",
        "-AenableLibraryCheck",
        "-AenableSideEffect",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {
      "ucrtainting/basicSubTypingTests",
      "ucrtainting/captureTest",
      "ucrtainting/javaUtilTest",
      "ucrtainting/micronaut",
      "ucrtainting/stringBuilderTests",
      "ucrtainting/thirdPartyTests",
      "ucrtainting/springSecOAuth",
      "ucrtainting/crashTests",
      "ucrtainting/esapiNullFieldTest"
    };
  }
}

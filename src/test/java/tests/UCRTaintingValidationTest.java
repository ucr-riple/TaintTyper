import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test runner for tests of the UCR Tainting Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/ucrtainting} folder. To add a new test case,
 * create a Java file in that directory. The file contains "// ::" comments to indicate expected
 * errors and warnings; see
 * https://github.com/typetools/checker-framework/blob/master/checker/tests/README .
 */
public class UCRTaintingValidationTest extends CheckerFrameworkPerDirectoryTest {
  public UCRTaintingValidationTest(List<File> testFiles) {
    super(
        testFiles,
        UCRTaintingChecker.class,
        "ucrtainting",
        "-Anomsgtext",
        "-AannotatedPackages=foo.bar",
        "-AenableLibraryCheck=false",
        "-AenableValidationCheck=true",
        "-AenableSanitizationCheck=false",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ucrtainting/validatorTests"};
  }
}

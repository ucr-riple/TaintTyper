package tests;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import java.io.File;
import java.util.List;
import org.junit.runners.Parameterized.Parameters;
import tests.tools.SerializationTestHelper;

public class LambdaInferenceTest extends SerializationTestHelper {
  public LambdaInferenceTest(List<File> testFiles) {
    super(
        testFiles,
        UCRTaintingChecker.class,
        "ucrtainting",
        "-Anomsgtext",
        "-AannotatedPackages=foo.bar",
        "-AenableLibraryCheck",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"lambdainference"};
  }
}

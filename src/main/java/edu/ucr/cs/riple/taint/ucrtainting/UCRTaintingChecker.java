package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/** This is the entry point for pluggable type-checking. */
@StubFiles({
  "stubs/Connection.astub",
  "stubs/apache.commons.io.astub",
  "stubs/apache.commons.lang.astub",
  "stubs/general.astub",
  "stubs/Files.astub",
  "stubs/taintedMethods.astub",
  "stubs/find-sec-bugs-sanitizers.astub",
  "stubs/StringBuffer.astub"
})
@SupportedOptions({
  UCRTaintingChecker.ANNOTATED_PACKAGES,
  UCRTaintingChecker.ENABLE_LIBRARY_CHECKER,
  UCRTaintingChecker.ENABLE_VALIDATION_CHECKER,
  UCRTaintingChecker.ENABLE_SANITIZATION_CHECKER
})
public class UCRTaintingChecker extends AccumulationChecker {

  /** Annotated packages config option for the checker. */
  /** Custom Library handling config option for the checker. */
  public static final String ENABLE_VALIDATION_CHECKER = "enableValidationCheck";

  public static final String ENABLE_LIBRARY_CHECKER = "enableLibraryCheck";
  public static final String ENABLE_SANITIZATION_CHECKER = "enableSanitizationCheck";
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";

  /** Serialization service for the checker. */
  //  private final SerializationService serializationService;
  //  /** Configuration for the checker. */
  //  private final Config config;
  //
  //  public UCRTaintingChecker() {
  //    this.config = new Config();
  //    serializationService = new SerializationService(config);
  //  }

  //  @Override
  //  public void reportWarning(Object source, @CompilerMessageKey String messageKey, Object...
  // args) {
  //    super.reportWarning(source, messageKey, args);
  //    serializationService.serializeError(
  //        source,
  //        messageKey,
  //        args,
  //        visitor,
  //        getTypeFactory(),
  //        ((JavacProcessingEnvironment) getProcessingEnvironment()).getContext());
  //  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    for (Object arg : args) {
      if (arg.toString().contains("capture#")) {
        return;
      }
    }
    super.reportError(source, messageKey, args);
    //      serializationService.serializeError(
    //          source,
    //          messageKey,
    //          args,
    //          visitor,
    //          getTypeFactory(),
    //          ((JavacProcessingEnvironment) getProcessingEnvironment()).getContext());
  }
}

package edu.ucr.cs.riple.taint.ucrtainting;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

import java.util.UUID;

/** This is the entry point for pluggable type-checking. */
@StubFiles({
  "stubs/Connection.astub",
  "stubs/apache.commons.io.astub",
  "stubs/apache.commons.lang.astub",
  "stubs/general.astub",
  "stubs/Files.astub",
  "stubs/taintedMethods.astub",
})
@SupportedOptions({UCRTaintingChecker.ANNOTATED_PACKAGES, UCRTaintingChecker.ENABLE_CUSTOM_CHECKER})
public class UCRTaintingChecker extends BaseTypeChecker {

  /** Annotated packages config option for the checker. */
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";
  /** Custom Library handling config option for the checker. */
  public static final String ENABLE_CUSTOM_CHECKER = "enableCustomCheck";
  /** Serialization service for the checker. */
  private SerializationService serializationService;

  public UCRTaintingChecker() {
  }

  @Override
  public void initChecker() {
    super.initChecker();
    this.serializationService = new SerializationService(this);
  }

  @Override
  public void reportWarning(Object source, @CompilerMessageKey String messageKey, Object... args) {
    super.reportWarning(source, messageKey, args);
    this.serializationService.serializeError(source, messageKey, args);
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    super.reportError(source, messageKey, args);
    this.serializationService.serializeError(source, messageKey, args);
  }
}

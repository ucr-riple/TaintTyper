package edu.ucr.cs.riple.taint.ucrtainting;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/** This is the entry point for pluggable type-checking. */
@StubFiles({
  "Connection.astub",
  "apache.commons.io.astub",
  "apache.commons.lang.astub",
  "general.astub",
})
public class UCRTaintingChecker extends BaseTypeChecker {

  private final SerializationService serializationService;

  public UCRTaintingChecker() {
    serializationService = new SerializationService();
  }

  @Override
  public void reportWarning(Object source, @CompilerMessageKey String messageKey, Object... args) {
    super.reportWarning(source, messageKey, args);
    serializationService.serializeError(source, messageKey, args, visitor);
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    super.reportError(source, messageKey, args);
    serializationService.serializeError(source, messageKey, args, visitor);
  }
}

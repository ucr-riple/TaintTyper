package edu.ucr.cs.riple.taint.ucrtainting;

import static edu.ucr.cs.riple.taint.ucrtainting.Log.print;

import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

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
  UCRTaintingChecker.ENABLE_SIDE_EFFECT,
  Config.SERIALIZATION_CONFIG_PATH,
  Config.SERIALIZATION_ACTIVATION_FLAG,
})
public class UCRTaintingChecker extends AccumulationChecker {

  public static int index = 0;

  public static final String ENABLE_VALIDATION_CHECKER = "enableValidationCheck";

  public static final String ENABLE_LIBRARY_CHECKER = "enableLibraryCheck";
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";

  public static final String ENABLE_SIDE_EFFECT = "enableSideEffect";
  /** Serialization service for the checker. */
  private SerializationService serializationService;

  private boolean serialize = true;

  public UCRTaintingChecker() {}

  @Override
  public void initChecker() {
    super.initChecker();
    this.serializationService = new SerializationService(this);
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    print("Index: " + ++index);
    super.reportError(source, messageKey, args);
    if (serialize) {
      try {
        FoundRequired pair = retrievePair(messageKey, args);
        this.serializationService.serializeError(source, messageKey, pair);
      } catch (Exception e) {
        print("Exception: " + e.getMessage());
      }
    }
  }

  public void detailedReportError(
      Object source, @CompilerMessageKey String messageKey, FoundRequired pair, Object... args) {
    this.serializationService.serializeError(source, messageKey, pair);
    this.serialize = false;
    reportError(source, messageKey, args);
    this.serialize = true;
  }

  private FoundRequired retrievePair(String messageKey, Object... args) {
    switch (messageKey) {
      case "override.return":
        {
          AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[5];
          AnnotatedTypeMirror.AnnotatedExecutableType overridingType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[3];
          return FoundRequired.of(overridingType.getReturnType(), overriddenType.getReturnType());
        }
      case "override.param":
        {
          AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[6];
          AnnotatedTypeMirror.AnnotatedExecutableType overridingType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[4];
          int index = 0;
          JCTree.JCMethodDecl i = (JCTree.JCMethodDecl) getVisitor().getCurrentPath().getLeaf();
          for (JCTree.JCVariableDecl arg : i.getParameters()) {
            if (arg.getName().toString().equals(args[0])) {
              break;
            }
            index++;
          }
          return FoundRequired.of(
              overriddenType.getParameterTypes().get(index),
              overridingType.getParameterTypes().get(index));
        }
      default:
        return null;
    }
  }
}

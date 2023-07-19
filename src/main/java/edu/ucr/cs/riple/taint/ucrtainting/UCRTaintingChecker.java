package edu.ucr.cs.riple.taint.ucrtainting;

import static edu.ucr.cs.riple.taint.ucrtainting.Log.print;

import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
})
@SupportedOptions({
  UCRTaintingChecker.ANNOTATED_PACKAGES,
  UCRTaintingChecker.ENABLE_CUSTOM_CHECKER,
  Config.SERIALIZATION_CONFIG_PATH,
  Config.SERIALIZATION_ACTIVATION_FLAG
})
public class UCRTaintingChecker extends BaseTypeChecker {

  public static int index = 0;

  /** Annotated packages config option for the checker. */
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";
  /** Custom Library handling config option for the checker. */
  public static final String ENABLE_CUSTOM_CHECKER = "enableCustomCheck";
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
    super.reportError(source, messageKey, args);
    if (serialize) {
      FoundRequired pair = retrievePair(source, messageKey, args);
      this.serializationService.serializeError(source, messageKey, args, pair);
    }
    print("Index " + ++index);
  }

  public void detailedReportError(
      Object source, @CompilerMessageKey String messageKey, FoundRequired pair, Object... args) {
    this.serialize = false;
    reportError(source, messageKey, args);
    print("Last visited node: " + visitor.getCurrentPath().getLeaf());
    this.serializationService.serializeError(source, messageKey, args, pair);
    this.serialize = true;
  }

  private FoundRequired retrievePair(Object source, String messageKey, Object... args) {
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

package edu.ucr.cs.riple.taint.ucrtainting;

import static edu.ucr.cs.riple.taint.ucrtainting.Log.print;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import javax.lang.model.element.Element;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

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

  public static final String ENABLE_SIDE_EFFECT = "enableSideEffect";

  public static final String ANNOTATED_PACKAGES = "annotatedPackages";
  /** Serialization service for the checker. */
  private SerializationService serializationService;

  private Types types;
  private UCRTaintingAnnotatedTypeFactory typeFactory;
  private boolean serialize = true;

  public UCRTaintingChecker() {}

  @Override
  public void initChecker() {
    super.initChecker();
    this.serializationService = new SerializationService(this);
    this.typeFactory = (UCRTaintingAnnotatedTypeFactory) getTypeFactory();
    this.types =
        Types.instance(((JavacProcessingEnvironment) getProcessingEnvironment()).getContext());
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (shouldBeSkipped(source, messageKey)) {
      return;
    }
    args[args.length - 1] = args[0].toString() + ", index: " + ++index;
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
    if (shouldBeSkipped(source, messageKey)) {
      return;
    }
    this.serializationService.serializeError(source, messageKey, pair);
    this.serialize = false;
    this.reportError(source, messageKey, args);
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

  /**
   * Determine if the error should be skipped.
   *
   * @param source The source of the error.
   * @param messageKey The message key of the error.
   * @return True if the error should be skipped, false otherwise.
   */
  private boolean shouldBeSkipped(Object source, String messageKey) {
    Tree tree = (Tree) source;
    switch (messageKey) {
        // Skip errors that are caused by third-party code.
      case "override.return":
        {
          Symbol.MethodSymbol overridingMethod =
              (Symbol.MethodSymbol) TreeUtils.elementFromTree(visitor.getCurrentPath().getLeaf());
          return overridingMethod == null || typeFactory.isInThirdPartyCode(overridingMethod);
        }
        // Skip errors that are caused by third-party code.
      case "override.param":
        {
          Element treeElement = TreeUtils.elementFromTree(tree);
          if (treeElement == null) {
            return true;
          }
          Symbol.MethodSymbol overridingMethod =
              (Symbol.MethodSymbol) treeElement.getEnclosingElement();
          if (overridingMethod == null) {
            return true;
          }
          Symbol.MethodSymbol overriddenMethod =
              Utility.getClosestOverriddenMethod(overridingMethod, types);
          return overriddenMethod == null || typeFactory.isInThirdPartyCode(overriddenMethod);
        }
      default:
        return false;
    }
  }
}

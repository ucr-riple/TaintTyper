package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.ThirdPartyHandler;
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
  private FoundRequired pair = null;

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
    pair = pair == null ? retrievePair(messageKey, args) : pair;
    if (shouldBeSkipped(source, messageKey, pair)) {
      return;
    }
    if (serialize) {
      this.serializationService.serializeError(source, messageKey, pair);
    }
    args[args.length - 1] = args[args.length - 1].toString() + ", index: " + ++index;
    super.reportError(source, messageKey, args);
  }

  public void detailedReportError(
      Object source, @CompilerMessageKey String messageKey, FoundRequired pair, Object... args) {
    if (shouldBeSkipped(source, messageKey, pair)) {
      return;
    }
    this.serializationService.serializeError(source, messageKey, pair);
    this.serialize = false;
    this.pair = pair;
    this.reportError(source, messageKey, args);
    this.pair = null;
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
          return FoundRequired.of(
              overridingType.getReturnType(), overriddenType.getReturnType(), 0);
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
              overridingType.getParameterTypes().get(index),
              0);
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
   * @param pair The pair of found and required annotated type mirrors.
   * @return True if the error should be skipped, false otherwise.
   */
  private boolean shouldBeSkipped(Object source, String messageKey, FoundRequired pair) {
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
      case "assignment":
        {
          Tree errorTree = visitor.getCurrentPath().getLeaf();
          if (!(errorTree instanceof JCTree.JCVariableDecl)) {
            return false;
          }
          JCTree.JCVariableDecl errorVar = (JCTree.JCVariableDecl) errorTree;
          if (!(errorVar.getInitializer() instanceof JCTree.JCMethodInvocation)) {
            return false;
          }
          boolean isApplicable =
              ThirdPartyHandler.checkHeuristicApplicability(
                  (MethodInvocationTree) errorVar.getInitializer(), typeFactory);
          if (!isApplicable) {
            return false;
          }
          // check miss match is only try args which found is untainted, but required is tainted.
          if (pair == null) {
            return false;
          }
          if (!(pair.found instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)
              || !(pair.required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)
              || !(pair.found.getUnderlyingType() instanceof Type.ClassType)
              || !(pair.required.getUnderlyingType() instanceof Type.ClassType)) {
            return false;
          }
          AnnotatedTypeMirror.AnnotatedDeclaredType found =
              (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.found;
          AnnotatedTypeMirror.AnnotatedDeclaredType required =
              (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.required;
          Type.ClassType foundType = (Type.ClassType) found.getUnderlyingType();
          Type.ClassType requiredType = (Type.ClassType) required.getUnderlyingType();
          if (!foundType.tsym.equals(requiredType.tsym)) {
            // We do not want to handle complex cases for now.
            return false;
          }
          // all other mismatches should be ignored.
          return !typeFactory.hasUntaintedAnnotation(required)
              || typeFactory.hasUntaintedAnnotation(found);
        }
      case "argument":
        if (!(tree instanceof MethodInvocationTree)) {
          return false;
        }
        return ThirdPartyHandler.checkHeuristicApplicability(
            (MethodInvocationTree) tree, typeFactory);
      default:
        return false;
    }
  }
}

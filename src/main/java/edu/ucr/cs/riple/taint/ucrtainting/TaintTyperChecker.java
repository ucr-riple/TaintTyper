/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.taint.ucrtainting;

import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.UnannotatedCodeHandler;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This is the entry point for pluggable type-checking. */
@StubFiles({
  "stubs/apache.commons.io.astub",
  "stubs/apache.commons.lang.astub",
  "stubs/codeql.astub",
  "stubs/Connection.astub",
  "stubs/Files.astub",
  "stubs/find-sec-bugs-sanitizers.astub",
  "stubs/general.astub",
  "stubs/httpservletreq.astub",
  //  "stubs/StringBuffer.astub",
  "stubs/taintedMethods.astub",
  "stubs/tdmljp.astub",
})
@SupportedOptions({
  TaintTyperChecker.ANNOTATED_PACKAGES,
  TaintTyperChecker.ENABLE_LIBRARY_CHECKER,
  TaintTyperChecker.ENABLE_VALIDATION_CHECKER,
  TaintTyperChecker.ENABLE_SIDE_EFFECT,
  TaintTyperChecker.ENABLE_POLY_TAINT_INFERENCE,
  TaintTyperChecker.ENABLE_TYPE_ARGUMENT_INFERENCE,
  Config.SERIALIZATION_CONFIG_PATH,
  Config.SERIALIZATION_ACTIVATION_FLAG,
})
public class TaintTyperChecker extends AccumulationChecker {

  public static int index = 0;
  public static final String ENABLE_VALIDATION_CHECKER = "enableValidationCheck";
  public static final String ENABLE_LIBRARY_CHECKER = "enableLibraryCheck";
  public static final String ENABLE_POLY_TAINT_INFERENCE = "enablePolyTaintInference";
  public static final String ENABLE_TYPE_ARGUMENT_INFERENCE = "enableTypeArgumentInference";
  public static final String ENABLE_SIDE_EFFECT = "enableSideEffect";
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";
  /** Serialization service for the checker. */
  private SerializationService serializationService;

  private TaintTyperAnnotatedTypeFactory typeFactory;
  private boolean serialize = true;
  private FoundRequired pair = null;

  public TaintTyperChecker() {}

  @Override
  public void initChecker() {
    super.initChecker();
    this.serializationService = new SerializationService(this);
    this.typeFactory = (TaintTyperAnnotatedTypeFactory) getTypeFactory();
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    pair = pair == null ? retrievePair(messageKey, args) : pair;
    if (shouldBeSkipped(source, messageKey, pair, args)) {
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
    if (shouldBeSkipped(source, messageKey, pair, args)) {
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
   * @param args Arguments passed to checker to create the error message
   * @return True if the error should be skipped, false otherwise.
   */
  private boolean shouldBeSkipped(
      Object source, String messageKey, FoundRequired pair, Object[] args) {
    Tree tree = (Tree) source;
    if (source instanceof JCTree.JCTypeCast) {
      ExpressionTree expression = ((JCTree.JCTypeCast) source).getExpression();
      // check if expression is null literal
      if (expression.getKind() == NULL_LITERAL) {
        // skip errors where the cast is from null literal
        return true;
      }
    }
    // check if the expression is a member of annotation
    if (shouldBeSkippedForAnnotationMemberSelection(source)) {
      return true;
    }

    switch (messageKey) {
      case "lambda.param":
      case "enum.declaration":
        return true;
        // Skip errors that are caused by third-party code.
      case "override.return":
        {
          Symbol.MethodSymbol overridingMethod =
              (Symbol.MethodSymbol) TreeUtils.elementFromTree(visitor.getCurrentPath().getLeaf());
          return overridingMethod == null || typeFactory.isUnannotatedMethod(overridingMethod);
        }
        // Skip errors that are caused by third-party code.
      case "override.param":
        {
          if (!(args[6] instanceof AnnotatedTypeMirror.AnnotatedExecutableType)) {
            return false;
          }
          AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[6];
          Symbol.MethodSymbol overrideMethod = (Symbol.MethodSymbol) overriddenType.getElement();
          return overrideMethod == null || typeFactory.isUnannotatedMethod(overrideMethod);
        }
      case "assignment":
      case "return":
        {
          Tree errorTree = visitor.getCurrentPath().getLeaf();
          ExpressionTree initializer = null;
          if (errorTree instanceof JCTree.JCReturn) {
            initializer = ((JCTree.JCReturn) errorTree).getExpression();
          }
          if (errorTree instanceof JCTree.JCVariableDecl) {
            initializer = ((JCTree.JCVariableDecl) errorTree).getInitializer();
          }
          if (errorTree instanceof JCTree.JCAssign) {
            initializer = ((JCTree.JCAssign) errorTree).getExpression();
          }
          if (!(initializer instanceof MethodInvocationTree)) {
            return false;
          }
          boolean isApplicable =
              UnannotatedCodeHandler.isSafeTransitionToUnAnnotatedCode(
                  (MethodInvocationTree) initializer, typeFactory);
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
        if (source instanceof ExpressionTree
            && TreeUtils.isExplicitThisDereference((ExpressionTree) source)) {
          return true;
        }
        // Check if the tree is a argument of a method invocation which the invocation is a third
        // party
        if (visitor.getCurrentPath().getLeaf() instanceof JCTree.JCMethodInvocation) {
          JCTree.JCMethodInvocation methodInvocation =
              (JCTree.JCMethodInvocation) visitor.getCurrentPath().getLeaf();
          Symbol.MethodSymbol methodSymbol =
              (Symbol.MethodSymbol) TreeUtils.elementFromUse(methodInvocation);
          if (methodSymbol != null && typeFactory.isUnannotatedMethod(methodSymbol)) {
            // we want to silence errors where the mismatch is found: List<@RUntainted String> and
            // required: List<@Tainted String>
            if (pair != null) {
              if (typeFactory.mayBeTainted(pair.found)
                  && !typeFactory.mayBeTainted(pair.required)) {
                return false;
              }
              if (pair.found instanceof AnnotatedTypeMirror.AnnotatedDeclaredType
                  && pair.required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
                AnnotatedTypeMirror.AnnotatedDeclaredType found =
                    (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.found;
                AnnotatedTypeMirror.AnnotatedDeclaredType required =
                    (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.required;
                boolean isSubtype = typeFactory.allTypeArgumentsAreSubType(found, required);
                if (isSubtype) {
                  return true;
                }
              }
            }
          }
        }
        if (!(tree instanceof MethodInvocationTree)) {
          return false;
        }
        return UnannotatedCodeHandler.isSafeTransitionToUnAnnotatedCode(
            (MethodInvocationTree) tree, typeFactory);
      default:
        return false;
    }
  }

  private boolean shouldBeSkippedForAnnotationMemberSelection(Object source) {
    Tree exp = (Tree) source;
    while (exp instanceof JCTree.JCTypeCast) {
      exp = ((JCTree.JCTypeCast) exp).getExpression();
    }
    JCTree.JCFieldAccess fieldAccess = null;
    if (exp instanceof JCTree.JCFieldAccess) {
      fieldAccess = (JCTree.JCFieldAccess) exp;
    }
    if (exp instanceof JCTree.JCMethodInvocation) {
      JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) exp;
      if (methodInvocation.getMethodSelect() instanceof JCTree.JCFieldAccess) {
        fieldAccess = (JCTree.JCFieldAccess) methodInvocation.getMethodSelect();
      }
    }
    if (fieldAccess == null) {
      return false;
    }
    Symbol.ClassSymbol owner = fieldAccess.sym.enclClass();
    return owner != null && owner.isAnnotationType();
  }
}

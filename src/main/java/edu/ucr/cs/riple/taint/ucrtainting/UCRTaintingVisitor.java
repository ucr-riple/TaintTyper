package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.org.plumelib.util.ArraysPlume;

/** Visitor for the {@link UCRTaintingChecker}. */
public class UCRTaintingVisitor extends AccumulationVisitor {

  /**
   * Creates a {@link UCRTaintingVisitor}.
   *
   * @param checker the checker that uses this visitor
   */
  public UCRTaintingVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Don't check that the constructor result is top. Checking that the super() or this() call is a
   * subtype of the constructor result is sufficient.
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    commonAssignmentCheckStartDiagnostic(varType, valueType, valueTree);

    AnnotatedTypeMirror widenedValueType = atypeFactory.getWidenedType(valueType, varType);
    boolean success = atypeFactory.getTypeHierarchy().isSubtype(widenedValueType, varType);
    // Use an error key only if it's overridden by a checker.
    if (!success) {
      FoundRequired pair = FoundRequired.of(valueType, varType, 0);
      String valueTypeString = pair.foundString;
      String varTypeString = pair.requiredString;
      ((UCRTaintingChecker) checker)
          .detailedReportError(
              valueTree,
              errorKey,
              pair,
              ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }
    return success;
  }

  public AnnotatedExecutableType getAnnotatedTypeOfOverriddenMethod(
      ExecutableElement methodElement) {
    AnnotatedExecutableType firsAnswer = null;
    // Find which methods this method overrides
    Map<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
        AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
    for (Map.Entry<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> pair :
        overriddenMethods.entrySet()) {
      AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType = pair.getKey();
      ExecutableElement overriddenMethodElt = pair.getValue();
      if (firsAnswer == null) {
        firsAnswer =
            AnnotatedTypes.asMemberOf(types, atypeFactory, overriddenType, overriddenMethodElt);
      }
      if (((Symbol.MethodSymbol) overriddenMethodElt).enclClass().sourcefile != null) {
        return firsAnswer;
      }
    }
    return firsAnswer;
  }
}

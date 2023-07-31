package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.Tree;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
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
  protected void commonAssignmentCheck(
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
      FoundRequired pair = FoundRequired.of(valueType, varType);
      String valueTypeString = pair.foundString;
      String varTypeString = pair.requiredString;
      ((UCRTaintingChecker) checker)
          .detailedReportError(
              valueTree,
              errorKey,
              pair,
              ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }
  }
}

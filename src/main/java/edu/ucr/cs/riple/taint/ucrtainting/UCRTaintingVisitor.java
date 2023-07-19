package edu.ucr.cs.riple.taint.ucrtainting;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

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
}

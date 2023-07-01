package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.Tree;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.org.plumelib.util.ArraysPlume;

/** Visitor for the {@link UCRTaintingChecker}. */
public class UCRTaintingVisitor extends BaseTypeVisitor<UCRTaintingAnnotatedTypeFactory> {

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
      String valueTypeString = pair.found;
      String varTypeString = pair.required;
      ((UCRTaintingChecker) checker)
          .detailedReportError(
              valueTree,
              errorKey,
              valueType,
              varType,
              ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }
  }

  /**
   * Class that creates string representations of {@link AnnotatedTypeMirror}s which are only
   * verbose if required to differentiate the two types.
   */
  private static class FoundRequired {
    public final String found;
    public final String required;

    private FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
      this.found = found.toString(true);
      this.required = required.toString(true);
    }

    /** Create a FoundRequired for a type and bounds. */
    private FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeParameterBounds required) {
      this.found = found.toString();
      this.required = required.toString();
    }

    /**
     * Creates string representations of {@link AnnotatedTypeMirror}s which are only verbose if
     * required to differentiate the two types.
     */
    static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
      return new FoundRequired(found, required);
    }

    /**
     * Creates string representations of {@link AnnotatedTypeMirror} and {@link
     * AnnotatedTypeParameterBounds}s which are only verbose if required to differentiate the two
     * types.
     */
    static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeParameterBounds required) {
      return new FoundRequired(found, required);
    }
  }
}

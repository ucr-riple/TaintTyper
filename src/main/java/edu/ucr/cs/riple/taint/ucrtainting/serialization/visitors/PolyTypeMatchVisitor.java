package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class PolyTypeMatchVisitor extends TypeMatchVisitor {
  public PolyTypeMatchVisitor(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public List<List<Integer>> defaultAction(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required, Void unused) {
    throw new UnsupportedOperationException(
        "Did not expect type match of found:"
            + found.getKind()
            + ":"
            + found
            + " with required:"
            + required.getKind()
            + ":"
            + required);
  }

  protected List<List<Integer>> supportedDefault(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    if (!typeFactory.hasPolyTaintedAnnotation(found)
        && typeFactory.hasPolyTaintedAnnotation(required)) {
      // e.g. @Polytainted int
      return List.of(List.of(0));
    }
    return List.of();
  }

  @Override
  public List<List<Integer>> visitDeclared_Declared(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    List<List<Integer>> result = new ArrayList<>();
    // e.g. @Polytainted String
    if (!typeFactory.hasPolyTaintedAnnotation(found)
        && typeFactory.hasPolyTaintedAnnotation(required)) {
      result.add(List.of(0));
    }
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return result;
    }
    for (int i = 0; i < required.getTypeArguments().size(); i++) {
      AnnotatedTypeMirror typeArgumentFound = found.getTypeArguments().get(i);
      AnnotatedTypeMirror typeArgumentRequired = required.getTypeArguments().get(i);
      List<Integer> toAddOnThisTypeArg = new ArrayList<>();
      toAddOnThisTypeArg.add(i + 1);
      List<List<Integer>> onTypeArgs = visit(typeArgumentFound, typeArgumentRequired, unused);
      for (List<Integer> toAddOnContainingTypeArg : onTypeArgs) {
        // Need a fresh chain for each type.
        if (!toAddOnContainingTypeArg.isEmpty()) {
          List<Integer> toAddOnThisTypeArgWithContainingTypeArgs =
              new ArrayList<>(toAddOnThisTypeArg);
          toAddOnThisTypeArgWithContainingTypeArgs.addAll(toAddOnContainingTypeArg);
          result.add(toAddOnThisTypeArgWithContainingTypeArgs);
        }
      }
    }
    return result;
  }
}

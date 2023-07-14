package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;

/**
 * Visitor for computing the required set of annotations on the declaration of an element which can
 * match the found annotated type mirror to the required annotated type mirror.
 */
public class TypeMatchVisitor extends AbstractAtmComboVisitor<List<List<Integer>>, Void> {

  private final UCRTaintingAnnotatedTypeFactory typeFactory;

  public TypeMatchVisitor(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super();
    this.typeFactory = typeFactory;
  }

  @Override
  protected String defaultErrorMessage(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void unused) {
    return null;
  }

  @Override
  protected List<List<Integer>> defaultAction(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void unused) {
    throw new UnsupportedOperationException(
        "Did not expect type match of "
            + type1.getKind()
            + ":"
            + type1
            + " and "
            + type2.getKind()
            + ":"
            + type2);
  }

  private List<List<Integer>> supportedDefault(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    List<List<Integer>> result = new ArrayList<>();
    if (!typeFactory.hasUntaintedAnnotation(found)
        && typeFactory.hasUntaintedAnnotation(required)) {
      // e.g. @Untainted int
      result.add(List.of(0));
    }
    return result;
  }

  @Override
  public List<List<Integer>> visitTypevar_Typevar(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedTypeVariable required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public List<List<Integer>> visitDeclared_Declared(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    List<List<Integer>> result = new ArrayList<>();
    // e.g. @Untainted String
    if (!typeFactory.hasUntaintedAnnotation(found)
        && typeFactory.hasUntaintedAnnotation(required)) {
      result.add(List.of(0));
    }
    for (int i = 0; i < required.getTypeArguments().size(); i++) {
      AnnotatedTypeMirror typeArgumentFound = found.getTypeArguments().get(i);
      AnnotatedTypeMirror typeArgumentRequired = required.getTypeArguments().get(i);
      if (typeArgumentFound.equals(typeArgumentRequired)) {
        // We do not need to continue this branch.
        continue;
      }
      List<Integer> toAddOnThisTypeArg = new ArrayList<>();
      toAddOnThisTypeArg.add(i + 1);
      if (typeFactory.hasUntaintedAnnotation(typeArgumentRequired)
          && !typeFactory.hasUntaintedAnnotation(typeArgumentFound)) {
        // e.g. @Untainted List<@Untainted String>
        result.add(List.of(1 + i, 0));
      }
      List<List<Integer>> unTypeArgs =
          new TypeMatchVisitor(typeFactory).visit(typeArgumentFound, typeArgumentRequired, unused);
      for (List<Integer> toAddOnContainingTypeArg : unTypeArgs) {
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

  @Override
  public List<List<Integer>> visitPrimitive_Primitive(
      AnnotatedTypeMirror.AnnotatedPrimitiveType found,
      AnnotatedTypeMirror.AnnotatedPrimitiveType required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public List<List<Integer>> visitWildcard_Wildcard(
      AnnotatedTypeMirror.AnnotatedWildcardType found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return this.visit(found.getExtendsBound(), required.getExtendsBound(), unused);
  }

  @Override
  public List<List<Integer>> visitTypevar_Wildcard(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return new TypeMatchVisitor(typeFactory).visit(found, required.getExtendsBound(), unused);
  }

  @Override
  public List<List<Integer>> visitTypevar_Declared(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public List<List<Integer>> visitDeclared_Wildcard(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return this.visit(found, required.getExtendsBound(), unused);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.tools.javac.code.Type;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;

/**
 * Visitor for computing the required set of annotations on the declaration of an element which can
 * match the found annotated type mirror to the required annotated type mirror.
 */
public class TypeMatchVisitor extends AbstractAtmComboVisitor<List<List<Integer>>, Void> {

  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  public TypeMatchVisitor(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super();
    this.typeFactory = typeFactory;
  }

  @Override
  public String defaultErrorMessage(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void unused) {
    return null;
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
    if (!typeFactory.hasUntaintedAnnotation(found)
        && typeFactory.hasUntaintedAnnotation(required)) {
      // e.g. @Untainted int
      return List.of(List.of(0));
    }
    return List.of();
  }

  @Override
  public List<List<Integer>> visitDeclared_Primitive(
      AnnotatedTypeMirror.AnnotatedDeclaredType type1,
      AnnotatedTypeMirror.AnnotatedPrimitiveType type2,
      Void unused) {
    return supportedDefault(type1, type2);
  }

  @Override
  public List<List<Integer>> visitWildcard_Declared(
      AnnotatedTypeMirror.AnnotatedWildcardType type1,
      AnnotatedTypeMirror.AnnotatedDeclaredType type2,
      Void unused) {
    return supportedDefault(type1, type2);
  }

  @Override
  public List<List<Integer>> visitTypevar_Typevar(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedTypeVariable required,
      Void unused) {
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
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
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
    return this.visit(found.getExtendsBound(), required.getExtendsBound(), unused);
  }

  @Override
  public List<List<Integer>> visitTypevar_Wildcard(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
    return this.visit(found, required.getExtendsBound(), unused);
  }

  @Override
  public List<List<Integer>> visitTypevar_Declared(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
    return supportedDefault(found, required);
  }

  @Override
  public List<List<Integer>> visitDeclared_Typevar(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedTypeVariable required,
      Void unused) {
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
    return supportedDefault(found, required);
  }

  @Override
  public List<List<Integer>> visitDeclared_Wildcard(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      return List.of();
    }
    return this.visit(found, required.getExtendsBound(), unused);
  }

  @Override
  public List<List<Integer>> visitArray_Array(
      AnnotatedTypeMirror.AnnotatedArrayType found,
      AnnotatedTypeMirror.AnnotatedArrayType required,
      Void unused) {
    // We only need to match the component type. Reference types are untainted by default.
    return this.visit(found.getComponentType(), required.getComponentType(), unused);
  }

  @Override
  public List<List<Integer>> visitPrimitive_Declared(
      AnnotatedTypeMirror.AnnotatedPrimitiveType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    TypeMirror requiredType = required.getUnderlyingType();
    if (!(requiredType instanceof Type.ClassType)) {
      return defaultAction(found, required, unused);
    }
    return ((Type.ClassType) requiredType).tsym.name.toString().equals("String")
        ? supportedDefault(found, required)
        : defaultAction(found, required, unused);
  }
}

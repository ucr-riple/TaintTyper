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

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;

/**
 * Visitor for computing the required set of annotations on the declaration of an element which can
 * match the found annotated type mirror to the required annotated type mirror.
 */
public class TypeMatchVisitor extends AbstractAtmComboVisitor<Set<TypeIndex>, Void> {

  private final Predicate<AnnotatedTypeMirror> predicate;

  private TypeMatchVisitor(Predicate<AnnotatedTypeMirror> predicate) {
    super();
    this.predicate = predicate;
  }

  public static TypeMatchVisitor createPolyTaintedMatcher(
      UCRTaintingAnnotatedTypeFactory typeFactory) {
    return new TypeMatchVisitor(typeFactory::hasPolyTaintedAnnotation);
  }

  public static TypeMatchVisitor createUntaintedMatcher(
      UCRTaintingAnnotatedTypeFactory typeFactory) {
    return new TypeMatchVisitor(typeFactory::hasUntaintedAnnotation);
  }

  @Override
  public String defaultErrorMessage(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void unused) {
    return null;
  }

  @Override
  public Set<TypeIndex> defaultAction(
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

  protected Set<TypeIndex> supportedDefault(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    if (!predicate.test(found) && predicate.test(required)) {
      return TypeIndex.topLevel();
    }
    return Collections.emptySet();
  }

  @Override
  public Set<TypeIndex> visitArray_Declared(
      AnnotatedTypeMirror.AnnotatedArrayType type1,
      AnnotatedTypeMirror.AnnotatedDeclaredType type2,
      Void unused) {
    return supportedDefault(type1.getComponentType(), type2);
  }

  @Override
  public Set<TypeIndex> visitWildcard_Typevar(
      AnnotatedTypeMirror.AnnotatedWildcardType type1,
      AnnotatedTypeMirror.AnnotatedTypeVariable type2,
      Void unused) {
    return supportedDefault(type1.getExtendsBound(), type2);
  }

  @Override
  public Set<TypeIndex> visitDeclared_Primitive(
      AnnotatedTypeMirror.AnnotatedDeclaredType type1,
      AnnotatedTypeMirror.AnnotatedPrimitiveType type2,
      Void unused) {
    return supportedDefault(type1, type2);
  }

  @Override
  public Set<TypeIndex> visitWildcard_Declared(
      AnnotatedTypeMirror.AnnotatedWildcardType type1,
      AnnotatedTypeMirror.AnnotatedDeclaredType type2,
      Void unused) {
    return supportedDefault(type1, type2);
  }

  @Override
  public Set<TypeIndex> visitTypevar_Typevar(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedTypeVariable required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public Set<TypeIndex> visitDeclared_Declared(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    Set<TypeIndex> result = new HashSet<>();
    // e.g. @Untainted String
    if (!predicate.test(found) && predicate.test(required)) {
      result.add(TypeIndex.TOP_LEVEL);
    }
    for (int i = 0; i < required.getTypeArguments().size(); i++) {
      if (i >= found.getTypeArguments().size() || i >= required.getTypeArguments().size()) {
        // It is possible that one of the types does not have type arguments.
        break;
      }
      AnnotatedTypeMirror typeArgumentFound = found.getTypeArguments().get(i);
      AnnotatedTypeMirror typeArgumentRequired = required.getTypeArguments().get(i);
      TypeIndex toAddOnThisTypeArg = TypeIndex.of(i + 1);
      Set<TypeIndex> onTypeArgs = visit(typeArgumentFound, typeArgumentRequired, unused);
      for (TypeIndex toAddOnContainingTypeArg : onTypeArgs) {
        // Need a fresh chain for each type.
        if (!toAddOnContainingTypeArg.isEmpty()) {
          TypeIndex realPosition = toAddOnContainingTypeArg.relativeTo(toAddOnThisTypeArg);
          result.add(realPosition);
        }
      }
    }
    return result;
  }

  @Override
  public Set<TypeIndex> visitPrimitive_Primitive(
      AnnotatedTypeMirror.AnnotatedPrimitiveType found,
      AnnotatedTypeMirror.AnnotatedPrimitiveType required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public Set<TypeIndex> visitWildcard_Wildcard(
      AnnotatedTypeMirror.AnnotatedWildcardType found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return this.visit(found.getExtendsBound(), required.getExtendsBound(), unused);
  }

  @Override
  public Set<TypeIndex> visitTypevar_Wildcard(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return this.visit(found, required.getExtendsBound(), unused);
  }

  @Override
  public Set<TypeIndex> visitTypevar_Declared(
      AnnotatedTypeMirror.AnnotatedTypeVariable found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public Set<TypeIndex> visitDeclared_Typevar(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedTypeVariable required,
      Void unused) {
    return supportedDefault(found, required);
  }

  @Override
  public Set<TypeIndex> visitDeclared_Wildcard(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedWildcardType required,
      Void unused) {
    return this.visit(found, required.getExtendsBound(), unused);
  }

  @Override
  public Set<TypeIndex> visitArray_Array(
      AnnotatedTypeMirror.AnnotatedArrayType found,
      AnnotatedTypeMirror.AnnotatedArrayType required,
      Void unused) {
    // We only need to match the component type. Reference types are untainted by default.
    return this.visit(found.getComponentType(), required.getComponentType(), unused);
  }

  @Override
  public Set<TypeIndex> visitPrimitive_Declared(
      AnnotatedTypeMirror.AnnotatedPrimitiveType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    return supportedDefault(found, required);
  }
}

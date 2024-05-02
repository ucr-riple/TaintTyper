package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.GenericMethodFixVisitor.locateInheritedTypeOnExtendOrImplement;

import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.ClassDeclarationLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;

public abstract class SpecializedFixComputer extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  protected final FixComputer fixComputer;

  protected final TypeMatchVisitor untaintedTypeMatchVisitor;
  protected final Context context;

  public SpecializedFixComputer(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    this.context = context;
    this.typeFactory = typeFactory;
    this.fixComputer = fixComputer;
    this.untaintedTypeMatchVisitor = TypeMatchVisitor.createUntaintedMatcher(typeFactory);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    Set<TypeIndex> indices;
    try {
      indices = untaintedTypeMatchVisitor.visit(pair.found, pair.required, null);
    } catch (IndexOutOfBoundsException e) {
      Type type = TypeUtils.getType(element);
      Symbol.ClassSymbol classType = (Symbol.ClassSymbol) type.tsym;
      Type.ClassType requiredType =
          (Type.ClassType) ((Type.ClassType) pair.required.getUnderlyingType()).tsym.type;
      // We intentionally limit the search to only the first level of inheritance. The type must
      // either extend or implement the required type explicitly at the declaration.
      Type.ClassType inheritedType =
          locateInheritedTypeOnExtendOrImplement(classType, requiredType);
      if (inheritedType == null) {
        return null;
      }
      ClassDeclarationLocation classDeclarationLocation =
          new ClassDeclarationLocation(classType, inheritedType);
      Set<AnnotatedTypeMirror.AnnotatedDeclaredType> supers =
          AnnotatedTypes.getSuperTypes((AnnotatedTypeMirror.AnnotatedDeclaredType) pair.found);
      supers.stream()
          .filter(
              annotatedDeclaredType -> {
                if (annotatedDeclaredType.getUnderlyingType() instanceof Type.ClassType) {
                  Type.ClassType classType1 =
                      (Type.ClassType) annotatedDeclaredType.getUnderlyingType();
                  return classType1.tsym.equals(inheritedType.tsym);
                }
                return false;
              })
          .findFirst()
          .ifPresent(
              annotatedDeclaredType -> {
                classDeclarationLocation.setTypeIndexSet(
                    untaintedTypeMatchVisitor.visit(annotatedDeclaredType, pair.required, null));
              });
      return new Fix(classDeclarationLocation);
    }

    AnnotatedTypeMirror elementAnnotatedType = typeFactory.getAnnotatedType(element);
    // remove redundant indices.
    indices =
        indices.stream()
            .filter(
                integers ->
                    !typeFactory.hasUntaintedAnnotation(
                        TypeUtils.getAnnotatedTypeMirrorOfTypeArgumentAt(
                            elementAnnotatedType, integers)))
            .collect(Collectors.toSet());
    location.setTypeIndexSet(indices);
    if (indices.isEmpty()) {
      return null;
    }
    return new Fix(location);
  }

  /**
   * Builds the location for the given element.
   *
   * @param element The element to build the location for.
   * @return The location for the given element.
   */
  protected SymbolLocation buildLocationForElement(Element element) {
    if (element == null) {
      return null;
    }
    return SymbolLocation.createLocationFromSymbol((Symbol) element, context);
  }
}

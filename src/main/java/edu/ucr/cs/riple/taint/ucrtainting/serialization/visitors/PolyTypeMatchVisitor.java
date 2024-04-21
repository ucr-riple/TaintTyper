package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class PolyTypeMatchVisitor extends TypeMatchVisitor {
  public PolyTypeMatchVisitor(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  protected Set<TypeIndex> supportedDefault(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    if (!typeFactory.hasPolyTaintedAnnotation(found)
        && typeFactory.hasPolyTaintedAnnotation(required)) {
      // e.g. @Polytainted int
      return TypeIndex.topLevelSet();
    }
    return Collections.emptySet();
  }

  @Override
  public Set<TypeIndex> visitDeclared_Declared(
      AnnotatedTypeMirror.AnnotatedDeclaredType found,
      AnnotatedTypeMirror.AnnotatedDeclaredType required,
      Void unused) {
    Set<TypeIndex> result = new HashSet<>();
    // e.g. @Polytainted String
    if (!typeFactory.hasPolyTaintedAnnotation(found)
        && typeFactory.hasPolyTaintedAnnotation(required)) {
      result.add(TypeIndex.TOP_LEVEL);
    }
    for (int i = 0; i < required.getTypeArguments().size(); i++) {
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
}

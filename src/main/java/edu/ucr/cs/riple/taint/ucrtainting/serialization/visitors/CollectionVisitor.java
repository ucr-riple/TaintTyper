package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getType;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CollectionHandler;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class CollectionVisitor extends ReceiverTypeParameterFixVisitor {

  public CollectionVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    super(context, factory, pair);
  }

  @Nullable
  public Fix buildFixForElement(Element element) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    if (getType(element).allparams().isEmpty()) {
      // receiver is written as a raw type and not parameterized. We cannot infer the actual types
      // and have to annotate the method directly.
      Set<Fix> fixes =
          receivers
              .get(receivers.size() - 1)
              .accept(new BasicVisitor(context, typeFactory, null), null);
      if (fixes != null && !fixes.isEmpty()) {
        return fixes.iterator().next();
      }
    }
    List<List<Integer>> indexes =
        locateEffectiveTypeParameter(element, new CollectionTypeMatchVisitor(typeFactory));
    location.setTypeVariablePositions(indexes);
    return new Fix(location);
  }

  static class CollectionTypeMatchVisitor extends TypeMatchVisitor {
    public CollectionTypeMatchVisitor(UCRTaintingAnnotatedTypeFactory factory) {
      super(factory);
    }

    @Override
    public List<List<Integer>> visitDeclared_Array(
        AnnotatedTypeMirror.AnnotatedDeclaredType found,
        AnnotatedTypeMirror.AnnotatedArrayType required,
        Void unused) {
      Type type = CollectionHandler.getSymbolicCollectionTypeFromType(found);
      if (!(type instanceof Type.ClassType)) {
        return List.of();
      }
      String typeVarName = ((Type.ClassType) type).typarams_field.get(0).tsym.name.toString();
      Type.ClassType foundClass = (Type.ClassType) found.getUnderlyingType();
      int index =
          foundClass.tsym.type.getTypeArguments().stream()
              .map(t -> t.tsym.name.toString())
              .collect(Collectors.toList())
              .indexOf(typeVarName);
      return List.of(List.of(index, 0));
    }
  }
}

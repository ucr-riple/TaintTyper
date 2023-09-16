package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getType;

import com.sun.tools.javac.code.Symbol;
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

public class CollectionVisitor extends ReceiverTypeArgumentFixVisitor {

  public CollectionVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer) {
    super(context, factory, fixComputer);
  }

  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    Type elementType = getType(element);
    int index = getCollectionTypeArgumentIndex(elementType);
    AnnotatedTypeMirror found = typeFactory.getAnnotatedType(element);
    AnnotatedTypeMirror required = found.deepCopy(true);
    if (!(required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      throw new RuntimeException("Not a declared type");
    }
    typeFactory.makeUntainted(
        ((AnnotatedTypeMirror.AnnotatedDeclaredType) required).getTypeArguments().get(index));
    if (elementType.allparams().isEmpty()) {
      // receiver is written as a raw type and not parameterized. We cannot infer the actual types
      // and have to annotate the method directly.
      Set<Fix> fixes =
          receivers
              .get(receivers.size() - 1)
              .accept(new BasicVisitor(context, typeFactory, fixComputer), null);
      if (fixes != null && !fixes.isEmpty()) {
        return fixes.iterator().next();
      }
    }
    List<List<Integer>> indexes = typeMatchVisitor.visit(found, required, null);
    location.setTypeVariablePositions(indexes);
    return new Fix(location);
  }

  private int getCollectionTypeArgumentIndex(Type type) {
    Type current = type.tsym.type;
    while (current instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) current;
      if (classType.interfaces_field != null) {
        for (Type iFace : classType.interfaces_field) {
          if (iFace.tsym instanceof Symbol.ClassSymbol
              && ((Symbol.ClassSymbol) iFace.tsym)
                  .fullname
                  .toString()
                  .equals("java.util.Collection")) {
            String name = iFace.getTypeArguments().get(0).toString();
            for (int i = 0; i < type.tsym.type.getTypeArguments().size(); i++) {
              if (type.tsym.type.getTypeArguments().get(i).toString().equals(name)) {
                return i;
              }
            }
          }
        }
      }
      Type superType = ((Type.ClassType) current).supertype_field;
      if (!(superType instanceof Type.ClassType)) {
        // todo: FIX
        return 0;
      }
      current = superType;
    }
    throw new RuntimeException("Not found");
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
      return List.of(List.of(index + 1, 0));
    }
  }
}

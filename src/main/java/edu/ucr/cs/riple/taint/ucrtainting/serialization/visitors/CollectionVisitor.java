package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;

public class CollectionVisitor extends ReceiverTypeArgumentFixVisitor {

  public CollectionVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
  }

  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    //    Type elementType = getType(element);
    //    SymbolLocation location = buildLocationForElement(element);
    //    if (location == null || !(elementType instanceof Type.ClassType)) {
    //      return null;
    //    }
    //    TypeArgumentRegion region = locateEffectiveTypeArgumentRegion(element);
    //    int index = getCollectionTypeArgumentIndex(region.type);
    //    AnnotatedTypeMirror found = typeFactory.getAnnotatedType(element);
    //    AnnotatedTypeMirror required = found.deepCopy(true);
    //    if (!(required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
    //      throw new RuntimeException("Not a declared type");
    //    }
    //    typeFactory.makeUntainted(
    //        ((AnnotatedTypeMirror.AnnotatedDeclaredType)
    //                Utility.getAnnotatedTypeMirrorOfTypeArgumentAt(required, region.index))
    //            .getTypeArguments()
    //            .get(index));
    //    if (elementType.allparams().isEmpty()) {
    //      // receiver is written as a raw type and not parameterized. We cannot infer the actual
    // types
    //      // and have to annotate the method directly.
    //      Set<Fix> fixes =
    //          receivers
    //              .get(receivers.size() - 1)
    //              .accept(new BasicVisitor(typeFactory, fixComputer, context), null);
    //      if (fixes != null && !fixes.isEmpty()) {
    //        return fixes.iterator().next();
    //      }
    //    }
    //    List<List<Integer>> indexes = typeMatchVisitor.visit(found, required, null);
    //    location.setTypeVariablePositions(indexes);
    //    return new Fix(location);
    return null;
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
        break;
      }
      current = superType;
    }
    throw new RuntimeException("Not found");
  }
}

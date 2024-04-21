package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;

/** Provides method for symbol locations. */
public interface SymbolLocation {

  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @param context Context instance.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  @Nullable
  static SymbolLocation createLocationFromSymbol(@Nullable Symbol target, Context context) {
    if (target == null) {
      return null;
    }
    switch (target.getKind()) {
      case PARAMETER:
        MethodParameterLocation methodParameterLocation = new MethodParameterLocation(target);
        return methodParameterLocation.index == -1 ? null : methodParameterLocation;
      case METHOD:
        return new MethodLocation(target);
      case FIELD:
        FieldLocation onField = new FieldLocation(target);
        if (onField.variableSymbol.name.toString().equals("class")) {
          // technically not a field.
          return null;
        }
        return onField;
      case LOCAL_VARIABLE:
      case RESOURCE_VARIABLE:
        JCTree declarationTree = Utility.locateDeclaration(target, context);
        LocalVariableLocation location = new LocalVariableLocation(target, declarationTree);
        // TODO: add support for making a local variable in a lambda static. For now let's skip.
        if (Serializer.serializeSymbol(location.enclosingMethod).equals("<clinit>")) {
          return null;
        }
        return location;
      case EXCEPTION_PARAMETER:
        // currently not supported / desired.
        return null;
      default:
        throw new IllegalArgumentException(
            "Cannot locate node: " + target + ", kind: " + target.getKind());
    }
  }

  /**
   * Applies a visitor to this location.
   *
   * @param <R> the return type of the visitor's methods
   * @param <P> the type of the additional parameter to the visitor's methods
   * @param v the visitor operating on this type
   * @param p additional parameter to the visitor
   * @return a visitor-specified result
   */
  <R, P> R accept(LocationVisitor<R, P> v, P p);

  void setTypeIndexSet(Set<TypeIndex> typeVariables);

  LocationKind getKind();

  Symbol getTarget();

  Path path();

  Set<TypeIndex> getTypeIndexSet();
}

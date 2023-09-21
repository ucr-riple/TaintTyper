package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.List;
import javax.annotation.Nullable;

/** Provides method for symbol locations. */
public interface SymbolLocation {

  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @param context Context.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  @Nullable
  static SymbolLocation createLocationFromSymbol(Symbol target, Context context) {
    JCTree declarationTree = Utility.locateDeclaration(target, context);
    switch (target.getKind()) {
      case PARAMETER:
        return new MethodParameterLocation(target, declarationTree);
      case METHOD:
        return new MethodLocation(target, declarationTree);
      case FIELD:
        return new FieldLocation(target, declarationTree);
      case LOCAL_VARIABLE:
        return new LocalVariableLocation(target, declarationTree);
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

  void setTypeVariablePositions(List<List<Integer>> typeVariables);

  LocationKind getKind();

  Symbol getTarget();
}

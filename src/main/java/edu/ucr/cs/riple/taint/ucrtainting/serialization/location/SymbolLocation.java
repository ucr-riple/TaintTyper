package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;

/** Provides method for symbol locations. */
public interface SymbolLocation {
  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @param context Context.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  static SymbolLocation createLocationFromSymbol(Symbol target, Context context) {
    JCTree declarationTree = Utility.locateDeclaration(target, context);
    int pos = declarationTree != null ? declarationTree.getStartPosition() : -1;
    switch (target.getKind()) {
      case PARAMETER:
        return new MethodParameterLocation(target);
      case METHOD:
        return new MethodLocation(target);
      case FIELD:
        return new FieldLocation(target);
      case LOCAL_VARIABLE:
        return new LocalVariableLocation(target, pos);
      default:
        throw new IllegalArgumentException("Cannot locate node: " + target);
    }
  }
}

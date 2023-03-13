package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.JSONSerializable;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;

/** Provides method for symbol locations. */
public interface SymbolLocation extends JSONSerializable {

  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @param context Context.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  static SymbolLocation createLocationFromSymbol(Symbol target, Context context, Type type) {
    JCTree declarationTree = Utility.locateDeclaration(target, context);
    switch (target.getKind()) {
      case PARAMETER:
        return new MethodParameterLocation(target, declarationTree, type);
      case METHOD:
        return new MethodLocation(target, declarationTree, type);
      case FIELD:
        return new FieldLocation(target, declarationTree, type);
      case LOCAL_VARIABLE:
        return new LocalVariableLocation(target, declarationTree, type);
      default:
        throw new IllegalArgumentException("Cannot locate node: " + target);
    }
  }
}

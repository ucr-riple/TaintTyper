package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;

/** Provides method for symbol locations. */
public interface SymbolLocation {
  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  static SymbolLocation createLocationFromSymbol(Symbol target) {
    switch (target.getKind()) {
      case PARAMETER:
        return new MethodParameterLocation(target);
      case METHOD:
        return new MethodLocation(target);
      case FIELD:
        return new FieldLocation(target);
      default:
        throw new IllegalArgumentException("Cannot locate node: " + target);
    }
  }
}

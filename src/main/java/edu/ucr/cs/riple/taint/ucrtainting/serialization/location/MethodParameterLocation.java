package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.google.common.base.Preconditions;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting a method parameter. */
public class MethodParameterLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  private final Symbol.MethodSymbol enclosingMethod;
  /** Symbol of the targeted method parameter. */
  private final Symbol.VarSymbol paramSymbol;
  /** Index of the method parameter in the containing method's argument list. */
  private final int index;

  public MethodParameterLocation(Symbol target) {
    super(ElementKind.PARAMETER, target);
    this.paramSymbol = (Symbol.VarSymbol) target;
    Symbol cursor = target;
    // Look for the enclosing method.
    while (cursor != null
        && cursor.getKind() != ElementKind.CONSTRUCTOR
        && cursor.getKind() != ElementKind.METHOD) {
      cursor = cursor.owner;
    }
    Preconditions.checkArgument(cursor instanceof Symbol.MethodSymbol);
    this.enclosingMethod = (Symbol.MethodSymbol) cursor;
    int i;
    for (i = 0; i < this.enclosingMethod.getParameters().size(); i++) {
      if (this.enclosingMethod.getParameters().get(i).equals(target)) {
        break;
      }
    }
    index = i;
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  protected final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target) {
    super(ElementKind.METHOD, target);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }
}

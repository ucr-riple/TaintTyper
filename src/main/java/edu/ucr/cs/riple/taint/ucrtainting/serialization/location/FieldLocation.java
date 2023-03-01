package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting class fields. */
public class FieldLocation extends AbstractSymbolLocation {

  /** Symbol of targeted class field */
  protected final Symbol.VarSymbol variableSymbol;

  public FieldLocation(Symbol target) {
    super(ElementKind.FIELD, target);
    variableSymbol = (Symbol.VarSymbol) target;
  }
}

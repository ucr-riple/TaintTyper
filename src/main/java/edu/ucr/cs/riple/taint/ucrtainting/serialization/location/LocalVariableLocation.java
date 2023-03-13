package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

public class LocalVariableLocation extends AbstractSymbolLocation {

  public LocalVariableLocation(Symbol target) {
    super(ElementKind.LOCAL_VARIABLE, target);
  }
}

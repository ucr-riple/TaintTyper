package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;

/** subtype of {@link AbstractSymbolLocation} targeting class fields. */
public class FieldLocation extends AbstractSymbolLocation {

  /** Symbol of targeted class field */
  public final Symbol.VarSymbol variableSymbol;

  public FieldLocation(Symbol target) {
    super(LocationKind.FIELD, target);
    variableSymbol = (Symbol.VarSymbol) target;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitField(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    FieldLocation that = (FieldLocation) o;
    return Objects.equals(variableSymbol, that.variableSymbol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), variableSymbol);
  }
}

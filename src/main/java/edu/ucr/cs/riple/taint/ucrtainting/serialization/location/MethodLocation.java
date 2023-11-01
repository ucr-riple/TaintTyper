package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  public final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target) {
    super(LocationKind.METHOD, target);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitMethod(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MethodLocation that = (MethodLocation) o;
    return Objects.equals(enclosingMethod, that.enclosingMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;

/** subtype of {@link AbstractSymbolLocation} targeting a method parameter declaration */
public class MethodParameterLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  public final Symbol.MethodSymbol enclosingMethod;
  /** Symbol of the targeted method parameter. */
  public final Symbol.VarSymbol paramSymbol;
  /** Index of the method parameter in the containing method's argument list. */
  public final int index;

  public MethodParameterLocation(Symbol target, Symbol.MethodSymbol enclosingMethod) {
    super(LocationKind.PARAMETER, target);
    this.paramSymbol = (Symbol.VarSymbol) target;
    this.enclosingMethod = enclosingMethod;
    int i;
    for (i = 0; i < this.enclosingMethod.getParameters().size(); i++) {
      if (this.enclosingMethod.getParameters().get(i).equals(target)) {
        break;
      }
    }
    this.index = i;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitParameter(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodParameterLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MethodParameterLocation that = (MethodParameterLocation) o;
    return index == that.index
        && Objects.equals(enclosingMethod, that.enclosingMethod)
        && Objects.equals(paramSymbol, that.paramSymbol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod, paramSymbol, index);
  }
}

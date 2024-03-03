package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;
import java.util.Set;

public class PolyMethodLocation extends AbstractSymbolLocation {

  public final Set<MethodParameterLocation> arguments;

  public PolyMethodLocation(MethodLocation location, Set<MethodParameterLocation> arguments) {
    super(LocationKind.POLY_METHOD, location.target);
    this.setTypeVariablePositions(location.typeVariablePositions);
    this.arguments = arguments;
    if (arguments.isEmpty()) {
      throw new RuntimeException("PolyMethodLocation must have at least one argument");
    }
    arguments.forEach(
        methodParameterLocation -> {
          if (!methodParameterLocation.enclosingMethod.equals(location.enclosingMethod)) {
            throw new RuntimeException(
                "PolyMethodLocation must have arguments from the same method");
          }
        });
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitPolyMethod(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PolyMethodLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    PolyMethodLocation that = (PolyMethodLocation) o;
    return Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), arguments);
  }

  @Override
  public String toString() {
    return super.toString() + ", arguments=" + arguments;
  }
}

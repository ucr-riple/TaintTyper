package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Set;
import javax.lang.model.element.ElementKind;

public class PolyMethodLocation extends AbstractSymbolLocation {

  public final Set<MethodParameterLocation> arguments;

  public PolyMethodLocation(MethodLocation location, Set<MethodParameterLocation> arguments) {
    super(ElementKind.OTHER, location.target, location.declarationTree);
    this.arguments = arguments;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitPolyMethod(this, p);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.OTHER;
  }
}

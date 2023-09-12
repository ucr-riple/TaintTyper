package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationToJsonVisitor;
import java.util.Objects;
import org.json.JSONObject;

/** This class represents an annotation to be added on an element to resolve an error. */
public class Fix implements JSONSerializable {

  /** Annotation to be added. */
  public final String annotation;
  /** Location of the element to be annotated. */
  public final SymbolLocation location;

  public Fix(String annotation, SymbolLocation location) {
    this.annotation = annotation;
    this.location = location;
  }

  public Fix(SymbolLocation location) {
    this("untainted", location);
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = new JSONObject();
    ans.put("annotation", annotation);
    ans.put("location", location.accept(new LocationToJsonVisitor(), null));
    return ans;
  }

  @Override
  public String toString() {
    return "Fix{" + "location=" + location + '}';
  }

  public Fix toPoly() {
    return new Fix("poly", location);
  }

  public boolean isPoly() {
    return location.getKind().isPoly();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Fix)) {
      return false;
    }
    Fix fix = (Fix) o;
    return Objects.equals(annotation, fix.annotation) && Objects.equals(location, fix.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotation, location);
  }
}

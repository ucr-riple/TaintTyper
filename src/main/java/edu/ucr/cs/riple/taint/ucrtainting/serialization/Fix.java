package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
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

  @Override
  public JSONObject toJSON() {
    JSONObject ans = new JSONObject();
    ans.put("annotation", annotation);
    ans.put("location", location);
    return ans;
  }
}

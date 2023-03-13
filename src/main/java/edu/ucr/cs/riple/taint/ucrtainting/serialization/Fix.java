package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;

/** This class represents an annotation to be added on an element to resolve an error. */
public class Fix {

  /** Annotation to be added. */
  public final String annotation;
  /** Location of the element to be annotated. */
  public final SymbolLocation location;

  public Fix(String annotation, SymbolLocation location) {
    this.annotation = annotation;
    this.location = location;
  }
}

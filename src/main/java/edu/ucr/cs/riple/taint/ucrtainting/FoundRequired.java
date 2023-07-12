package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Class that creates string representations of {@link AnnotatedTypeMirror}s which are only verbose
 * if required to differentiate the two types.
 */
public class FoundRequired {
  public final String foundString;
  public final String requiredString;
  public final AnnotatedTypeMirror found;
  public final AnnotatedTypeMirror required;

  public FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    this.found = found;
    this.required = required;
    this.foundString = found == null ? "" : found.toString(true);
    this.requiredString = required == null ? "" : required.toString(true);
  }

  /**
   * Creates string representations of {@link AnnotatedTypeMirror}s which are only verbose if
   * required to differentiate the two types.
   */
  public static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    return new FoundRequired(found, required);
  }
}

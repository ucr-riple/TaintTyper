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
  public int depth;
  public static final int MAX_DEPTH = 5;

  public FoundRequired(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    this.found = found;
    this.required = required;
    this.foundString = found == null ? "" : found.toString(true);
    this.requiredString = required == null ? "" : required.toString(true);
    this.depth = 0;
  }

  /**
   * Creates string representations of {@link AnnotatedTypeMirror}s which are only verbose if
   * required to differentiate the two types.
   */
  public static FoundRequired of(AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    return new FoundRequired(found, required);
  }

  public void incrementDepth() {
    this.depth++;
  }

  public boolean isMaxDepth() {
    return this.depth >= MAX_DEPTH;
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

/** Enum representing the kind of location of a symbol. */
public enum LocationKind {
  FIELD,
  PARAMETER,
  LOCAL_VARIABLE,
  METHOD,
  CLASS_DECLARATION,
  POLY_METHOD;

  /**
   * Returns true if the location kind is a field.
   *
   * @return true if the location kind is a field.
   */
  public boolean isField() {
    return this == FIELD;
  }

  /**
   * Returns true if the location kind is a parameter.
   *
   * @return true if the location kind is a parameter.
   */
  public boolean isParameter() {
    return this == PARAMETER;
  }

  /**
   * Returns true if the location kind is a method.
   *
   * @return true if the location kind is a method.
   */
  public boolean isMethod() {
    return this == METHOD;
  }

  /**
   * Returns true if the location kind is a poly method.
   *
   * @return true if the location kind is a poly method.
   */
  public boolean isPoly() {
    return this == POLY_METHOD;
  }

  /**
   * Returns true if the location kind is a local variable.
   *
   * @return true if the location kind is a local variable.
   */
  public boolean isLocalVariable() {
    return this == LOCAL_VARIABLE;
  }

  /** Returns true if the location kind is a class declaration */
  public boolean isClassDeclaration() {
    return this == CLASS_DECLARATION;
  }
}

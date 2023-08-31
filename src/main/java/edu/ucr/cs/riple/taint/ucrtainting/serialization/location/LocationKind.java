package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

public enum LocationKind {
  FIELD,
  PARAMETER,
  LOCAL_VARIABLE,
  METHOD,
  POLY_METHOD;

  public boolean isField() {
    return this == FIELD;
  }

  public boolean isParameter() {
    return this == PARAMETER;
  }

  public boolean isMethod() {
    return this == METHOD;
  }

  public boolean isPoly() {
    return this == POLY_METHOD;
  }

  public boolean isLocalVariable() {
    return this == LOCAL_VARIABLE;
  }
}

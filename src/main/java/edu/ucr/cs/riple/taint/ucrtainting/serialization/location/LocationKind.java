/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

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

import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;
import java.util.Set;

public class PolyMethodLocation extends AbstractSymbolLocation {

  public final Set<MethodParameterLocation> arguments;

  public PolyMethodLocation(MethodLocation location, Set<MethodParameterLocation> arguments) {
    super(LocationKind.POLY_METHOD, location.target);
    this.setTypeIndexSet(location.typeIndexSet);
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

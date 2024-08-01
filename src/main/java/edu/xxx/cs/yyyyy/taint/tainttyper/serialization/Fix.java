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

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization;

import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.SymbolLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors.LocationToJsonVisitor;
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
    return location.toString();
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

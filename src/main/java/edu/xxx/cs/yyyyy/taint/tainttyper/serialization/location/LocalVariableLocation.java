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

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors.LocationVisitor;
import java.util.Objects;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  public final Symbol.MethodSymbol enclosingMethod;

  public LocalVariableLocation(Symbol target, Symbol.MethodSymbol enclosingMethod) {
    super(LocationKind.LOCAL_VARIABLE, target);
    this.enclosingMethod = enclosingMethod;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocalVariableLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    LocalVariableLocation that = (LocalVariableLocation) o;
    return Objects.equals(enclosingMethod, that.enclosingMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod);
  }
}

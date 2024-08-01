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
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.Serializer;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.TypeIndex;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors.LocationVisitor;
import edu.xxx.cs.yyyyy.taint.tainttyper.util.SymbolUtils;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Interface for all symbol locations */
public interface SymbolLocation {

  /**
   * returns the appropriate subtype of {@link SymbolLocation} based on the target kind.
   *
   * @param target Target element.
   * @return subtype of {@link SymbolLocation} matching target's type.
   */
  @Nullable
  static SymbolLocation createLocationFromSymbol(@Nullable Symbol target) {
    if (target == null) {
      return null;
    }
    Symbol.MethodSymbol enclosingMethod = SymbolUtils.findEnclosingMethod(target);
    switch (target.getKind()) {
      case PARAMETER:
        if (enclosingMethod == null || isMainMethod(enclosingMethod)) {
          return null;
        }
        // check if the enclosing method has parameter with the given symbol name, otherwise, the
        // parameter is inside a lambda
        boolean hasArgumentWithTargetName =
            enclosingMethod.getParameters().stream()
                .anyMatch(param -> param.name.equals(target.name));
        return hasArgumentWithTargetName
            ? new MethodParameterLocation(target, enclosingMethod)
            : null;
      case METHOD:
        return new MethodLocation(target);
      case FIELD:
        FieldLocation onField = new FieldLocation(target);
        if (onField.variableSymbol.name.toString().equals("class")) {
          // technically not a field.
          return null;
        }
        return onField;
      case LOCAL_VARIABLE:
      case RESOURCE_VARIABLE:
        // For local variables in static initializer blocks, enclosingMethod is null.
        // We don't have the support yet.
        if (enclosingMethod == null
            || Serializer.serializeSymbol(enclosingMethod).equals("<clinit>")) {
          return null;
        }
        return new LocalVariableLocation(target, enclosingMethod);
      case EXCEPTION_PARAMETER:
        // currently not supported / desired.
        return null;
      default:
        throw new IllegalArgumentException(
            "Cannot locate node: " + target + ", kind: " + target.getKind());
    }
  }

  /**
   * Checks if the given method symbol is {@code public static void main(String[])} method.
   *
   * @param enclosingMethod The method symbol to check.
   * @return {@code true} if the given method symbol is {@code public static void main(String[])}
   *     method.
   */
  private static boolean isMainMethod(Symbol.MethodSymbol enclosingMethod) {
    // check if method is public
    if (!enclosingMethod.getModifiers().contains(Modifier.PUBLIC)) {
      return false;
    }
    // check if method is static
    if (!enclosingMethod.isStatic()) {
      return false;
    }
    // check if return type is void
    if (!enclosingMethod.getReturnType().toString().equals("void")) {
      return false;
    }
    // check if method name is main
    if (!enclosingMethod.getSimpleName().toString().equals("main")) {
      return false;
    }
    // check if method has a single parameter
    if (enclosingMethod.getParameters().size() != 1) {
      return false;
    }
    // check if the parameter is of type String[]
    return enclosingMethod.getParameters().get(0).asType().toString().equals("java.lang.String[]");
  }

  /**
   * Applies a visitor to this location.
   *
   * @param <R> the return type of the visitor's methods
   * @param <P> the type of the additional parameter to the visitor's methods
   * @param v the visitor operating on this type
   * @param p additional parameter to the visitor
   * @return a visitor-specified result
   */
  <R, P> R accept(LocationVisitor<R, P> v, P p);

  /**
   * Sets the type index set for this location.
   *
   * @param typeVariables The set of type indexes to set.
   */
  void setTypeIndexSet(Set<TypeIndex> typeVariables);

  /**
   * Gets the kind of this location.
   *
   * @return The kind of this location.
   */
  LocationKind getKind();

  /**
   * Returns the target symbol for the location.
   *
   * @return The target symbol for the location.
   */
  Symbol getTarget();

  /**
   * Returns the path to source file where the symbol is located.
   *
   * @return The path to source file where the symbol is located.
   */
  Path path();

  /**
   * Returns type index set for this location.
   *
   * @return The type index set for this location.
   */
  Set<TypeIndex> getTypeIndexSet();
}

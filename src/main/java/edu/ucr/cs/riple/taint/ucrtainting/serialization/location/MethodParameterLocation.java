package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.google.common.base.Preconditions;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** subtype of {@link AbstractSymbolLocation} targeting a method parameter. */
public class MethodParameterLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  public final Symbol.MethodSymbol enclosingMethod;
  /** Symbol of the targeted method parameter. */
  public final Symbol.VarSymbol paramSymbol;
  /** Index of the method parameter in the containing method's argument list. */
  public final int index;

  public MethodParameterLocation(Symbol target) {
    super(LocationKind.PARAMETER, target);
    this.paramSymbol = (Symbol.VarSymbol) target;
    Symbol cursor = target;
    // Look for the enclosing method.
    while (cursor != null
        && cursor.getKind() != ElementKind.CONSTRUCTOR
        && cursor.getKind() != ElementKind.METHOD) {
      cursor = cursor.owner;
    }
    Preconditions.checkArgument(cursor instanceof Symbol.MethodSymbol);
    this.enclosingMethod = (Symbol.MethodSymbol) cursor;
    int i;
    boolean success = false;
    for (i = 0; i < this.enclosingMethod.getParameters().size(); i++) {
      if (this.enclosingMethod.getParameters().get(i).equals(target)) {
        success = true;
        break;
      }
    }
    if (success) {
      // we want to avoid where the parameter is the main method.
      success = !isMainMethod(this.enclosingMethod);
    }
    this.index = success ? i : -1;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitParameter(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodParameterLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MethodParameterLocation that = (MethodParameterLocation) o;
    return index == that.index
        && Objects.equals(enclosingMethod, that.enclosingMethod)
        && Objects.equals(paramSymbol, that.paramSymbol);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod, paramSymbol, index);
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
}

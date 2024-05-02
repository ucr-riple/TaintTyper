package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Provides method for symbol locations. */
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
        if (Serializer.serializeSymbol(enclosingMethod).equals("<clinit>")) {
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

  void setTypeIndexSet(Set<TypeIndex> typeVariables);

  LocationKind getKind();

  Symbol getTarget();

  Path path();

  Set<TypeIndex> getTypeIndexSet();
}

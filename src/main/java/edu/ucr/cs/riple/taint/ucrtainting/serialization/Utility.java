package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.IdentifierTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/** Utility methods for the serialization service. */
public class Utility {

  public static final TypeArgumentVisitor TYPE_ARGUMENT_VISITOR = new TypeArgumentVisitor();

  private Utility() {
    // This class is mainly a collection of static methods and should not be instantiated.
  }

  /**
   * Locates the variable declaration tree for a given identifier tree which is a local variable in
   * a block. The identifier is assumed to not be a field or a method parameter.
   *
   * @param localVariable the identifier tree.
   * @param context the javac context.
   * @return the variable declaration tree or null if the variable declaration cannot be found.
   */
  @Nullable
  public static JCTree locateLocalVariableDeclaration(
      IdentifierTree localVariable, Context context) {
    Symbol sym = (Symbol) TreeUtils.elementFromTree(localVariable);
    if (sym == null) {
      return null;
    }
    Env<AttrContext> enterEnv = Enter.instance(context).getEnv(sym.enclClass());
    if (enterEnv == null) {
      return null;
    }
    return TreeInfo.declarationFor(sym, enterEnv.tree);
  }

  /**
   * find the closest ancestor method in a superclass or superinterface that method overrides
   *
   * @param method the subclass method
   * @param types the types data structure from javac
   * @return closest overridden ancestor method, or <code>null</code> if method does not override
   *     anything
   */
  public static Symbol.MethodSymbol getClosestOverriddenMethod(
      Symbol.MethodSymbol method, Types types) {
    // taken from Error Prone MethodOverrides check
    Symbol.ClassSymbol owner = method.enclClass();
    for (Type s : types.closure(owner.type)) {
      if (types.isSameType(s, owner.type)) {
        continue;
      }
      for (Symbol m : s.tsym.members().getSymbolsByName(method.name)) {
        if (!(m instanceof Symbol.MethodSymbol)) {
          continue;
        }
        Symbol.MethodSymbol memberSymbol = (Symbol.MethodSymbol) m;
        if (memberSymbol.isStatic()) {
          continue;
        }
        if (method.overrides(memberSymbol, owner, types, /*checkReturn*/ false)) {
          return memberSymbol;
        }
      }
    }
    return null;
  }

  public static List<Type.TypeVar> getTypeParametersInOrder(Type type) {
    return type.tsym.type.accept(TYPE_ARGUMENT_VISITOR, null);
  }

  static class TypeArgumentVisitor extends Types.DefaultTypeVisitor<List<Type.TypeVar>, Void> {

    @Override
    public List<Type.TypeVar> visitClassType(Type.ClassType type, Void unused) {
      return type.typarams_field.stream()
          .flatMap(t -> t.accept(this, null).stream())
          .collect(Collectors.toList());
    }

    @Override
    public List<Type.TypeVar> visitTypeVar(Type.TypeVar t, Void unused) {
      Type upperBound = t.getUpperBound();
      if (upperBound.toString().equals("java.lang.Object")) {
        return List.of(t);
      } else {
        return upperBound.accept(this, null);
      }
    }

    @Override
    public List<Type.TypeVar> visitType(Type type, Void unused) {
      return List.of();
    }
  }

  /**
   * Checks if a type contains a parameter type.
   *
   * @param type the type to check
   * @return true if the type contains a parameter type, false otherwise
   */
  public static boolean containsParameterType(Type type) {
    if (isTypeVar(type)) {
      return true;
    }
    if (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      for (Type t : classType.getTypeArguments()) {
        if (containsParameterType(t)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if a type is a parameter type.
   *
   * @param type the type to check
   * @return true if the type is a parameter type, false otherwise
   */
  public static boolean isTypeVar(Type type) {
    return type instanceof Type.TypeVar;
  }
}

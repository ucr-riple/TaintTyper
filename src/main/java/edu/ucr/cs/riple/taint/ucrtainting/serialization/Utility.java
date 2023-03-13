package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
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
import javax.annotation.Nullable;
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
   * Given a TreePath, finds the first enclosing node of the given type and returns the path from
   * the enclosing node to the top-level {@code CompilationUnitTree}.
   */
  public static <T> TreePath findPathFromEnclosingNodeToTopLevel(TreePath path, Class<T> klass) {
    if (path != null) {
      do {
        path = path.getParentPath();
      } while (path != null && !klass.isInstance(path.getLeaf()));
    }
    return path;
  }

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type. Returns null if no
   * such node is found.
   */
  @Nullable
  public static <T> T findEnclosingNode(TreePath path, Class<T> klass) {
    path = findPathFromEnclosingNodeToTopLevel(path, klass);
    return (path == null) ? null : klass.cast(path.getLeaf());
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

  @Nullable
  public static Symbol.ClassSymbol findRegionClassSymbol(TreePath path) {
    // If path is on a class, that class itself is the region class. Otherwise, use the enclosing
    // class.
    ClassTree classTree =
        path.getLeaf() instanceof ClassTree
            ? (ClassTree) path.getLeaf()
            : findEnclosingNode(path, ClassTree.class);
    return classTree != null
        ? (Symbol.ClassSymbol) TreeUtils.elementFromDeclaration(classTree)
        : null;
  }

  @Nullable
  public static Symbol findRegionMemberSymbol(
      @Nullable Symbol.ClassSymbol regionClass, TreePath path) {
    if (regionClass == null) {
      return null;
    }
    Symbol ans = null;
    MethodTree enclosingMethod;
    // If the error is reported on a method, that method itself is the relevant program point.
    // Otherwise, use the enclosing method (if present).
    enclosingMethod =
        path.getLeaf() instanceof MethodTree
            ? (MethodTree) path.getLeaf()
            : findEnclosingNode(path, MethodTree.class);
    if (enclosingMethod != null) {
      // It is possible that the computed method is not enclosed by the computed class, e.g., for
      // the following case:
      //  class C {
      //    void foo() {
      //      class Local {
      //        Object f = null; // error
      //      }
      //    }
      //  }
      // Here the above code will compute clazz to be Local and method as foo().  In such cases,
      // set method to null, we always want the corresponding method to be nested in the
      // corresponding class.
      Symbol.MethodSymbol methodSymbol =
          (Symbol.MethodSymbol) TreeUtils.elementFromDeclaration(enclosingMethod);
      if (methodSymbol != null && !methodSymbol.isEnclosedBy(regionClass)) {
        enclosingMethod = null;
      }
    }
    if (enclosingMethod != null) {
      ans = (Symbol) TreeUtils.elementFromDeclaration(enclosingMethod);
    } else {
      // Node is not enclosed by any method, can be a field declaration or enclosed by it.
      Symbol sym = (Symbol) TreeUtils.elementFromTree(path.getLeaf());
      Symbol.VarSymbol fieldSymbol = null;
      if (sym != null && sym.getKind().isField() && sym.isEnclosedBy(regionClass)) {
        // Directly on a field declaration.
        fieldSymbol = (Symbol.VarSymbol) sym;
      } else {
        // Can be enclosed by a field declaration tree.
        VariableTree fieldDeclTree = findEnclosingNode(path, VariableTree.class);
        if (fieldDeclTree != null) {
          fieldSymbol = (Symbol.VarSymbol) TreeUtils.elementFromDeclaration(fieldDeclTree);
        }
      }
      if (fieldSymbol != null && fieldSymbol.isEnclosedBy(regionClass)) {
        ans = fieldSymbol;
      }
    }
    return ans;
  }
}

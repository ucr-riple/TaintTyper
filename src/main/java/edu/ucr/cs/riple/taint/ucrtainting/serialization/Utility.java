package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.com.google.common.base.Objects;
import org.checkerframework.javacutil.TreeUtils;

/** Utility methods for the serialization service. */
public class Utility {

  private Utility() {
    // This class is mainly a collection of static methods and should not be instantiated.
  }

  /**
   * Locates the variable declaration tree for a given identifier tree which is a local variable in
   * a block. The identifier is assumed to not be a field or a method parameter.
   *
   * @param localVariable the identifier tree.
   * @param path the path to the identifier tree and the scope of the local variable.
   * @return the variable declaration tree or null if the variable declaration cannot be found.
   */
  @Nullable
  public static VariableTree locateLocalVariableDeclaration(
      IdentifierTree localVariable, TreePath path) {
    Symbol.VarSymbol symbol = (Symbol.VarSymbol) TreeUtils.elementFromTree(localVariable);
    if (symbol == null) {
      return null;
    }
    Tree owner = null;
    while (path != null) {
      if (Objects.equal(TreeUtils.elementFromTree(path.getLeaf()), symbol.owner)) {
        owner = path.getLeaf();
        break;
      }
      path = path.getParentPath();
    }
    if (owner == null) {
      return null;
    }
    TreeScanner<VariableTree, Name> treeScanner =
        new TreeScanner<VariableTree, Name>() {
          @Override
          public VariableTree visitVariable(VariableTree tree, Name name) {
            if (Objects.equal(tree.getName(), name)) {
              Symbol treeSym = (Symbol) TreeUtils.elementFromDeclaration(tree);
              // Check the scope here.
              if (treeSym != null && symbol.owner.equals(treeSym.owner)) {
                return tree;
              }
            }
            return super.visitVariable(tree, name);
          }

          @Override
          public VariableTree reduce(VariableTree r1, VariableTree r2) {
            // to keep the found variable tree alive in the scanner reduce method.
            if (r1 == null) {
              return r2;
            } else {
              return r1;
            }
          }
        };
    return treeScanner.scan(owner, localVariable.getName());
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
}

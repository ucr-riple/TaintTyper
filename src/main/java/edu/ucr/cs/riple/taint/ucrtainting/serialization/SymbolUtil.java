package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import static org.checkerframework.com.google.common.base.Preconditions.checkNotNull;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.com.google.common.base.Objects;

/** Utility methods for working with {@link Symbol}s. */
public class SymbolUtil {

  /** Gets the symbol for a tree. Returns null if this tree does not have a symbol. */
  @Nullable
  public static Symbol getDeclaredSymbol(Tree tree) {
    if (tree instanceof TypeParameterTree) {
      Type type = ((JCTree.JCTypeParameter) tree).type;
      return type == null ? null : type.tsym;
    }
    if (tree instanceof ClassTree) {
      return getSymbol((ClassTree) tree);
    }
    if (tree instanceof MethodTree) {
      return getSymbol((MethodTree) tree);
    }
    if (tree instanceof VariableTree) {
      return getSymbol((VariableTree) tree);
    }
    return null;
  }

  /**
   * Gets the symbol for a tree. Returns null if this tree does not have a symbol because it is of
   * the wrong type, if {@code tree} is null, or if the symbol cannot be found due to a compilation
   * error.
   */
  // TODO(eaftan): refactor other code that accesses symbols to use this method
  public static Symbol getSymbol(Tree tree) {
    if (tree instanceof AnnotationTree) {
      return getSymbol(((AnnotationTree) tree).getAnnotationType());
    }
    if (tree instanceof JCTree.JCFieldAccess) {
      return ((JCTree.JCFieldAccess) tree).sym;
    }
    if (tree instanceof JCTree.JCIdent) {
      return ((JCTree.JCIdent) tree).sym;
    }
    if (tree instanceof JCTree.JCMethodInvocation) {
      return SymbolUtil.getSymbol((MethodInvocationTree) tree);
    }
    if (tree instanceof JCTree.JCNewClass) {
      return SymbolUtil.getSymbol((NewClassTree) tree);
    }
    if (tree instanceof MemberReferenceTree) {
      return ((JCTree.JCMemberReference) tree).sym;
    }
    if (tree instanceof JCTree.JCAnnotatedType) {
      return getSymbol(((JCTree.JCAnnotatedType) tree).underlyingType);
    }
    if (tree instanceof ParameterizedTypeTree) {
      return getSymbol(((ParameterizedTypeTree) tree).getType());
    }
    if (tree instanceof ClassTree) {
      return getSymbol((ClassTree) tree);
    }

    return getDeclaredSymbol(tree);
  }

  /** Gets the symbol for a class. */
  public static Symbol.ClassSymbol getSymbol(ClassTree tree) {
    return checkNotNull(((JCTree.JCClassDecl) tree).sym, "%s had a null ClassSymbol", tree);
  }

  /** Gets the symbol for a method. */
  public static Symbol.MethodSymbol getSymbol(MethodTree tree) {
    return checkNotNull(((JCTree.JCMethodDecl) tree).sym, "%s had a null MethodSymbol", tree);
  }

  /** Gets the symbol for a variable. */
  public static Symbol.VarSymbol getSymbol(VariableTree tree) {
    return checkNotNull(((JCTree.JCVariableDecl) tree).sym, "%s had a null VariableTree", tree);
  }

  /** Gets the symbol for a method invocation. */
  public static Symbol.MethodSymbol getSymbol(MethodInvocationTree tree) {
    Symbol sym = SymbolUtil.getSymbol(tree.getMethodSelect());
    if (!(sym instanceof Symbol.MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      throw new IllegalArgumentException(tree.toString());
    }
    return (Symbol.MethodSymbol) sym;
  }

  /** Gets the symbol for a member reference. */
  public static Symbol.MethodSymbol getSymbol(MemberReferenceTree tree) {
    Symbol sym = ((JCTree.JCMemberReference) tree).sym;
    if (!(sym instanceof Symbol.MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      throw new IllegalArgumentException(tree.toString());
    }
    return (Symbol.MethodSymbol) sym;
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
    Symbol.VarSymbol symbol = (Symbol.VarSymbol) getSymbol(localVariable);
    if (symbol == null) {
      return null;
    }
    Tree owner = null;
    while (path != null) {
      if (Objects.equal(getSymbol(path.getLeaf()), symbol.owner)) {
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
              Symbol treeSym = getSymbol(tree);
              // Check the scope here.
              if (symbol.owner.equals(treeSym.owner)) {
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
}

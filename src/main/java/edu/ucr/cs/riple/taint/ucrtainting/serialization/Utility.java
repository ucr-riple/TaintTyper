package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.IdentifierTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import org.checkerframework.checker.nullness.qual.Nullable;
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
   * @param processingEnvironment the processing environment.
   * @return the variable declaration tree or null if the variable declaration cannot be found.
   */
  @Nullable
  public static JCTree locateLocalVariableDeclaration(
      IdentifierTree localVariable, JavacProcessingEnvironment processingEnvironment) {
    Symbol sym = (Symbol) TreeUtils.elementFromTree(localVariable);
    if (sym == null) {
      return null;
    }
    Env<AttrContext> enterEnv =
        Enter.instance(processingEnvironment.getContext()).getEnv(sym.enclClass());
    if (enterEnv == null) {
      return null;
    }
    return TreeInfo.declarationFor(sym, enterEnv.tree);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.source.SourceVisitor;

/** This class is used to serialize the errors and the fixes for the errors. */
public class SerializationService {

  /**
   * Generates the fixes for the given tree if exists.
   *
   * @param tree The given tree.
   * @param treeChecker The tree checker to check if a tree requires a fix.
   * @param path The path of the tree.
   */
  public Set<Fix> generateFixesForExpression(Tree tree, TreeChecker treeChecker, TreePath path) {
    FixVisitor fixVisitor = new FixVisitor(treeChecker, path);
    Set<Fix> resolvingFixes = new HashSet<>();
    fixVisitor.visit(tree, resolvingFixes);
    return resolvingFixes;
  }

  /**
   * This method is called when a warning or error is reported by the checker and serialized the
   * error along the set of required fixes to resolve the error if exists.
   *
   * @param source the source of the error
   * @param messageKey the key of the error message
   * @param args the arguments of the error message
   * @param visitor the visitor that is visiting the source
   */
  public void serializeError(
      Object source, String messageKey, Object[] args, SourceVisitor<?, ?> visitor) {
    Set<Fix> resolvingFixes =
        generateFixesForExpression((Tree) source, tree -> true, visitor.getCurrentPath());
    // TODO: serialize the error and the fixes, will be implemented in the next PR, once the format
    // is finalized.
  }
}

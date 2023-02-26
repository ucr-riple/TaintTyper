package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.com.google.common.collect.ImmutableSet;
import org.checkerframework.framework.source.SourceVisitor;

/** This class is used to serialize the errors and the fixes for the errors. */
public class SerializationService {

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
    // TODO: for TreeChecker instance below, use the actual API which checks if the tree is
    // @Tainted. For now, we pass tree -> true, to serialize a fix for all expressions on the right
    // hand side of the assignment.
    Tree sourceForFix = getSourceForFix(source, messageKey);
    Set<Fix> resolvingFixes =
        sourceForFix == null
            ? ImmutableSet.of()
            : generateFixesForExpression(sourceForFix, tree -> true, visitor.getCurrentPath());
    Error error = new Error(messageKey, String.format(messageKey, args), resolvingFixes);
    // TODO: serialize the error, will be implemented in the next PR, once the format
    // is finalized.
  }

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
   * Returns the source for the fix based on the given message key. For inheritance related errors,
   * the fix requires a change on the parent method, for all others the source itself is the root
   * for applying fixes.
   *
   * @param source the source of the error.
   * @param messageKey the key of the error message.
   * @return the source for the fix based on the given message key.
   */
  @Nullable
  private Tree getSourceForFix(Object source, String messageKey) {
    if (!(source instanceof Tree)) {
      // For all cases where the source is not a tree, we return null for now.
      return null;
    }
    switch (messageKey) {
      case "override.param":
      case "override.return":
        // TODO: for now, we return null, will be implemented in the next PR which handles
        // inheritance.
        return null;
      default:
        // For all other cases, the involved tree in the error is the source for fix.
        return (Tree) source;
    }
  }
}

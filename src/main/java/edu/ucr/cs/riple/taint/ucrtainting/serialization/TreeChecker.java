package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.Tree;

/** This interface is used to check if a tree requires a fix. */
public interface TreeChecker {
  /**
   * Checks if a tree requires a fix.
   *
   * @param tree the tree to check
   * @return true if the tree requires a fix, false otherwise
   */
  boolean check(Tree tree);
}

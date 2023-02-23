package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.Tree;

/** This class is used to serialize the errors and the fixes for the errors. */
public class Serializer {

  /** The serializer visitor. */
  private final SerializerVisitor serializerVisitor;

  public Serializer() {
    serializerVisitor = new SerializerVisitor();
  }

  /**
   * Generates the fixes for the given tree if exists.
   *
   * @param tree The given tree.
   * @param treeChecker The tree checker to check if a tree requires a fix.
   */
  public void generateFixesForExpression(Tree tree, TreeChecker treeChecker) {
    serializerVisitor.visit(tree, treeChecker);
  }

  /**
   * This method is called when a warning or error is reported by the checker and serialized the
   * error along the set of required fixes to resolve the error if exists.
   *
   * @param source the source of the error
   * @param messageKey the key of the error message
   * @param args the arguments of the error message
   */
  public void serializeError(Object source, String messageKey, Object[] args) {}
}

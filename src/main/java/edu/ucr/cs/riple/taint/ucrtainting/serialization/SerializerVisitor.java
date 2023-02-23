package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.util.SimpleTreeVisitor;

/** This class is used to serialize the fixes for trees and subtrees if they require a fix. */
public class SerializerVisitor extends SimpleTreeVisitor<Void, TreeChecker> {

  @Override
  public Void visitIdentifier(com.sun.source.tree.IdentifierTree node, TreeChecker checker) {
    return super.visitIdentifier(node, checker);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, TreeChecker checker) {
    return super.visitConditionalExpression(node, checker);
  }
}

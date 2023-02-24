package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.util.SimpleTreeVisitor;

/** This class is used to serialize the fixes for trees and subtrees if they require a fix. */
public class SerializerVisitor extends SimpleTreeVisitor<Void, TreeChecker> {

  @Override
  public Void visitIdentifier(IdentifierTree node, TreeChecker checker) {
    super.visitIdentifier(node, checker);
    //    identifierToVariableTree(node);
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, TreeChecker checker) {
    node.getFalseExpression().accept(this, checker);
    node.getTrueExpression().accept(this, checker);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, TreeChecker checker) {
    node.getMethodSelect().accept(this, checker);
    return null;
  }

  @Override
  public Void visitLiteral(LiteralTree node, TreeChecker checker) {
    // We do not generate fix for primitive types.
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, TreeChecker checker) {
    // We do not generate fix for primitive types.
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree node, TreeChecker checker) {
    node.getExpression().accept(this, checker);
    return null;
  }

  //  public static VariableTree identifierToVariableTree(IdentifierTree identifierTree) {
  //    Element element = TreeUtils.elementFromTree(identifierTree);
  //    if (element == null) {
  //      return null;
  //    }
  //    switch (element.getKind()) {
  //      case FIELD:
  //        System.out.println("FIELD: " + element);
  //        break;
  //      case PARAMETER:
  //        System.out.println("PARAMETER: " + element);
  //        break;
  //      case LOCAL_VARIABLE:
  //        System.out.println("LOCAL_VARIABLE: " + element);
  //        break;
  //    }
  //    return null;
  //  }
}

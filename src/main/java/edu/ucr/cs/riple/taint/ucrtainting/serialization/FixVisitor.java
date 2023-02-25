package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Void, Set<Fix>> {

  /** The tree checker to check if a tree requires a fix. */
  private final TreeChecker checker;

  /** The starting point of visitor. */
  private final TreePath path;

  public FixVisitor(TreeChecker checker, TreePath path) {
    this.checker = checker;
    this.path = path;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Set<Fix> fixes) {
    if (checker.check(node)) {
      buildFixForElement(node);
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, Set<Fix> fixes) {
    if (checker.check(node.getTrueExpression())) {
      node.getTrueExpression().accept(this, fixes);
    }
    if (checker.check(node.getFalseExpression())) {
      node.getFalseExpression().accept(this, fixes);
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Set<Fix> fixes) {
    if (checker.check(node.getMethodSelect())) {
      buildFixForElement(node.getMethodSelect());
    }
    return null;
  }

  @Override
  public Void visitLiteral(LiteralTree node, Set<Fix> fixes) {
    // We do not generate fix for primitive types.
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, Set<Fix> fixes) {
    // We do not generate fix for primitive types.
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree node, Set<Fix> fixes) {
    node.getExpression().accept(this, fixes);
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree node, Set<Fix> fixes) {
    node.getLeftOperand().accept(this, fixes);
    node.getRightOperand().accept(this, fixes);
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Set<Fix> fixes) {
    if (checker.check(node.getExpression())) {
      buildFixForElement(node);
    }
    return null;
  }

  @Override
  public Void visitUnary(UnaryTree node, Set<Fix> fixes) {
    node.getExpression().accept(this, fixes);
    return null;
  }

  /**
   * Builds the fix for the given element.
   *
   * @param tree The given tree.
   */
  public void buildFixForElement(Tree tree) {
    // TODO: make the actual fix instance here once the format is finalized.
    Element element = TreeUtils.elementFromTree(tree);
    if (element == null) {
      return;
    }
    switch (element.getKind()) {
      case FIELD:
        System.out.println("FIELD: " + element);
        break;
      case PARAMETER:
        System.out.println("PARAMETER: " + element);
        break;
      case LOCAL_VARIABLE:
        VariableTree variableTree =
            Utility.locateLocalVariableDeclaration((IdentifierTree) tree, path);
        System.out.println("LOCAL_VARIABLE: " + variableTree);
        break;
      case METHOD:
        System.out.println("METHOD: " + element);
        break;
    }
    // TODO: make the actual fix instance here once the format is finalized.
  }
}

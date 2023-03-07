package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.ArrayAccessTree;
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
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Type.TypeVar> {

  /** The tree checker to check if the type of the given tree is {@code @RTainted}. */
  private final TreeChecker checker;

  /** The javac context. */
  private final Context context;

  public FixVisitor(TreeChecker checker, Context context) {
    this.checker = checker;
    this.context = context;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Type.TypeVar typeVar) {
    if (checker.check(node)) {
      return Set.of(buildFixForElement(node, typeVar));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Type.TypeVar typeVar) {
    Set<Fix> fixes = new HashSet<>();
    if (checker.check(node.getTrueExpression())) {
      fixes.addAll(node.getTrueExpression().accept(this, typeVar));
    }
    if (checker.check(node.getFalseExpression())) {
      fixes.addAll(node.getFalseExpression().accept(this, typeVar));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Type.TypeVar typeVar) {
    if (checker.check(node.getMethodSelect())) {
      return Set.of(buildFixForElement(node.getMethodSelect(), typeVar));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitLiteral(LiteralTree node, Type.TypeVar typeVar) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitPrimitiveType(PrimitiveTypeTree node, Type.TypeVar typeVar) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitExpressionStatement(ExpressionStatementTree node, Type.TypeVar typeVar) {
    return node.getExpression().accept(this, typeVar);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Type.TypeVar typeVar) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getLeftOperand().accept(this, typeVar));
    fixes.addAll(node.getRightOperand().accept(this, typeVar));
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Type.TypeVar typeVar) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(this, typeVar);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Type.TypeVar typeVar) {
    if (checker.check(node.getExpression())) {
      return Set.of(buildFixForElement(node, typeVar));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Type.TypeVar typeVar) {
    return node.getExpression().accept(this, typeVar);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param tree The given tree.
   * @param typeVar The type variable.
   * @return The fix for the given element.
   */
  public Fix buildFixForElement(Tree tree, Type.TypeVar typeVar) {
    // TODO: make the actual fix instance here once the format is finalized.
    Element element = TreeUtils.elementFromTree(tree);
    if (element == null) {
      return null;
    }
    switch (element.getKind()) {
      case FIELD:
        System.out.println("FIELD: " + element);
        break;
      case PARAMETER:
        System.out.println("PARAMETER: " + element);
        break;
      case LOCAL_VARIABLE:
        JCTree variableTree =
                Utility.locateLocalVariableDeclaration((IdentifierTree) tree, context);
        System.out.println("LOCAL_VARIABLE: " + variableTree);
        break;
      case METHOD:
        System.out.println("METHOD: " + element);
        break;
    }
    // TODO: make the actual fix instance here once the format is finalized.
    return new Fix();
  }
}

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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Type> {

  /** The tree checker to check if the type of the given tree is {@code @RTainted}. */
  private final TreeChecker checker;

  /** The javac context. */
  private final Context context;

  public FixVisitor(TreeChecker checker, Context context) {
    this.checker = checker;
    this.context = context;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Type typeVar) {
    if (checker.check(node)) {
      return Set.of(buildFixForElement(node, typeVar));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    if (checker.check(node.getTrueExpression())) {
      fixes.addAll(node.getTrueExpression().accept(this, typeVar));
    }
    if (checker.check(node.getFalseExpression())) {
      fixes.addAll(node.getFalseExpression().accept(this, typeVar));
    }
    return fixes;
  }

  /**
   * Visitor for method invocations. For method invocations:
   *
   * <ol>
   *   <li>If return type is not type variable, we annotate the called method.
   *   <li>If return type is type variable and defined in source code, we annotate the called
   *       method.
   *   <li>If return type is type variable and defined in library, we annotate the receiver.
   * </ol>
   *
   * @param node The given tree.
   * @return Void null.
   */
  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Type type) {
    if (checker.check(node.getMethodSelect())) {
      Element element = TreeUtils.elementFromUse(node);
      if (element == null) {
        return null;
      }
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
      // check for type variable in return type.
      if (Utility.containsParameterType(methodSymbol.getReturnType())) {
        // set type, if not set.
        type = type == null ? methodSymbol.getReturnType() : type;
        // Build the fix for the receiver.
        return ((MemberSelectTree) node.getMethodSelect())
            .getExpression()
            .accept(this, type);
      } else {
        // Build a fix for the called method return type.
        return Set.of(buildFixForElement(node.getMethodSelect(), null));
      }
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitLiteral(LiteralTree node, Type type) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitPrimitiveType(PrimitiveTypeTree node, Type type) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitExpressionStatement(ExpressionStatementTree node, Type type) {
    return node.getExpression().accept(this, type);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Type type) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getLeftOperand().accept(this, type));
    fixes.addAll(node.getRightOperand().accept(this, type));
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Type type) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(this, type);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Type type) {
    if (checker.check(node.getExpression())) {
      return Set.of(buildFixForElement(node, type));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Type type) {
    return node.getExpression().accept(this, type);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param tree The given tree.
   * @param type The type variable.
   * @return The fix for the given element.
   */
  public Fix buildFixForElement(Tree tree, Type type) {
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
    if (type != null) {
      List<Type.TypeVar> vars = Utility.getTypeParametersInOrder(((Symbol) element).type);
      System.out.println(vars);
    }
    // TODO: make the actual fix instance here once the format is finalized.
    return new Fix();
  }
}

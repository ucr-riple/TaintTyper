package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This visitor directly annotates the element declaration to match the required type. */
public class BasicVisitor extends SpecializedFixComputer {

  protected final MethodReturnVisitor returnVisitor;

  public BasicVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    this.returnVisitor = new MethodReturnVisitor(typeFactory, fixComputer, context);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    if (requireFix(pair)) {
      Fix fix = buildFixForElement(TreeUtils.elementFromTree(node), pair);
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getTrueExpression().accept(fixComputer, pair));
    fixes.addAll(node.getFalseExpression().accept(fixComputer, pair));
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(NewClassTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      AnnotatedTypeMirror foundArgType = typeFactory.getAnnotatedType(arg);
      AnnotatedTypeMirror requiredArgType = foundArgType.deepCopy(true);
      typeFactory.makeUntainted(requiredArgType);
      fixes.addAll(
          arg.accept(fixComputer, new FoundRequired(foundArgType, requiredArgType, pair.depth)));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    if (node.getExpression() != null && typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(fixComputer, pair));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      node.getInitializers().forEach(arg -> fixes.addAll(arg.accept(fixComputer, pair)));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    Fix onMethod = buildFixForElement(calledMethod, pair);
    if (onMethod == null || !requireFix(pair)) {
      return Set.of();
    }
    if (pair.isMaxDepth()) {
      return Set.of(onMethod);
    }
    if (calledMethod.getParameters().isEmpty()) {
      // no parameters, make untainted
      return Set.of(onMethod);
    }
    JCTree decl = Utility.locateDeclaration(calledMethod, context);
    if (decl == null || decl.getKind() != Tree.Kind.METHOD) {
      return Set.of(onMethod);
    }
    JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) decl;
    if (methodDecl.getBody() == null) {
      return Set.of(onMethod);
    }
    if (methodDecl.params.isEmpty()) {
      return Set.of(onMethod);
    }
    if (!typeFactory.polyTaintInferenceEnabled()) {
      return Set.of(onMethod);
    }
    returnVisitor.addInvocation(node, pair);
    if (returnVisitor.getState(calledMethod).equals(MethodReturnVisitor.STATE.VISITING)) {
      // create for arguments
      return Set.of();
    }
    Set<Fix> fixesOnDecl = new HashSet<>(methodDecl.accept(returnVisitor, pair));
    Set<Fix> tmp =
        returnVisitor.computeFixesForArgumentsOnInferredPolyTaintedMethods(
            calledMethod, fixesOnDecl, pair);
    return tmp;
  }

  @Override
  public Set<Fix> visitLiteral(LiteralTree node, FoundRequired pair) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitPrimitiveType(PrimitiveTypeTree node, FoundRequired pair) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitExpressionStatement(ExpressionStatementTree node, FoundRequired pair) {
    return node.getExpression().accept(fixComputer, pair);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    ExpressionTree left = node.getLeftOperand();
    ExpressionTree right = node.getRightOperand();
    AnnotatedTypeMirror leftType = typeFactory.getAnnotatedType(left);
    AnnotatedTypeMirror rightType = typeFactory.getAnnotatedType(right);
    FoundRequired leftPair;
    FoundRequired rightPair;
    JCTree.Tag tag = ((JCTree.JCBinary) node).getTag();
    if (tag == JCTree.Tag.EQ || tag == JCTree.Tag.NE) {
      AnnotatedTypeMirror leftCopy = leftType.deepCopy(true);
      typeFactory.makeUntainted(leftCopy);
      AnnotatedTypeMirror rightCopy = rightType.deepCopy(true);
      typeFactory.makeUntainted(rightCopy);
      leftPair = FoundRequired.of(leftType, leftCopy, pair.depth);
      rightPair = FoundRequired.of(rightType, rightCopy, pair.depth);
    } else {
      leftPair = FoundRequired.of(leftType, pair.required, pair.depth);
      rightPair = FoundRequired.of(rightType, pair.required, pair.depth);
    }
    fixes.addAll(left.accept(fixComputer, leftPair));
    fixes.addAll(right.accept(fixComputer, rightPair));
    return fixes;
  }

  @Override
  public Set<Fix> visitArrayAccess(ArrayAccessTree node, FoundRequired pair) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(fixComputer, pair);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
    if (requireFix(pair)) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }
      Fix fix = buildFixForElement(member, pair);
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitParenthesized(ParenthesizedTree node, FoundRequired pair) {
    return node.getExpression().accept(fixComputer, pair);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, FoundRequired pair) {
    return node.getExpression().accept(fixComputer, pair);
  }

  @Override
  public Set<Fix> visitCompoundAssignment(CompoundAssignmentTree node, FoundRequired pair) {
    return node.getExpression().accept(fixComputer, pair);
  }

  @Override
  public Set<Fix> visitVariable(VariableTree node, FoundRequired foundRequired) {
    Fix onVariable = buildFixForElement(TreeUtils.elementFromDeclaration(node), foundRequired);
    if (!requireFix(foundRequired)) {
      return Set.of();
    }
    return onVariable == null ? Set.of() : Set.of(onVariable);
  }

  /**
   * Checks if the fix is required.
   *
   * @return True if the fix is required, false otherwise.
   */
  protected boolean requireFix(FoundRequired pair) {
    if (pair == null) {
      return true;
    }
    AnnotatedTypeMirror widenedValueType = typeFactory.getWidenedType(pair.found, pair.required);
    try {
      return !typeFactory.getTypeHierarchy().isSubtype(widenedValueType, pair.required);
    } catch (Exception e) {
      return true;
    }
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class BasicVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  protected final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  @Nullable protected final AnnotatedTypeMirror required;

  public BasicVisitor(
      Context context,
      UCRTaintingAnnotatedTypeFactory factory,
      @Nullable AnnotatedTypeMirror required) {
    this.context = context;
    this.typeFactory = factory;
    this.required = required;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Void unused) {
    if (typeFactory.mayBeTainted(node)) {
      Fix fix = buildFixForElement(TreeUtils.elementFromTree(node));
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getTrueExpression())) {
      fixes.addAll(
          node.getTrueExpression().accept(new FixVisitor(context, typeFactory, null), null));
    }
    if (typeFactory.mayBeTainted(node.getFalseExpression())) {
      fixes.addAll(
          node.getFalseExpression().accept(new FixVisitor(context, typeFactory, null), null));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(com.sun.source.tree.NewClassTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(new FixVisitor(context, typeFactory, null), unused));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(new FixVisitor(context, typeFactory, null), unused));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      for (ExpressionTree arg : node.getInitializers()) {
        if (typeFactory.mayBeTainted(arg)) {
          fixes.addAll(arg.accept(new FixVisitor(context, typeFactory, null), unused));
        }
      }
    }
    // Add a fix for each dimension.
    for (ExpressionTree arg : node.getDimensions()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(new FixVisitor(context, typeFactory, null), unused));
      }
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
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Void unused) {
    Element element = TreeUtils.elementFromUse(node);
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
  }

  @Override
  public Set<Fix> visitLiteral(LiteralTree node, Void unused) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitExpressionStatement(ExpressionStatementTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, null), unused);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getLeftOperand().accept(new FixVisitor(context, typeFactory, null), null));
    fixes.addAll(node.getRightOperand().accept(new FixVisitor(context, typeFactory, null), null));
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Void unused) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(new FixVisitor(context, typeFactory, null), unused);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Void unused) {
    if (typeFactory.mayBeTainted(node.getExpression())) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }

      Fix fix = buildFixForElement(TreeUtils.elementFromUse(node));
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitParenthesized(ParenthesizedTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, null), unused);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, null), unused);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element) {
    SymbolLocation location;
    if (element == null) {
      return null;
    }
    location = SymbolLocation.createLocationFromSymbol((Symbol) element, context);
    if (location == null) {
      return null;
    }
    return new Fix("untainted", location);
  }
}

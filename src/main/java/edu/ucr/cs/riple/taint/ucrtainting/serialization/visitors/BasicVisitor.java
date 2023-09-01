package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
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

/** This visitor directly annotates the element declaration to match the required type. */
public class BasicVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  protected final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  @Nullable protected final FoundRequired pair;

  public BasicVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    this.context = context;
    this.typeFactory = factory;
    this.pair = pair;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Void unused) {
    if (node != null && typeFactory.mayBeTainted(node)) {
      Fix fix = buildFixForElement(TreeUtils.elementFromTree(node));
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (node.getTrueExpression() != null && typeFactory.mayBeTainted(node.getTrueExpression())) {
      fixes.addAll(
          node.getTrueExpression().accept(new FixVisitor(context, typeFactory, pair), null));
    }
    if (node.getFalseExpression() != null && typeFactory.mayBeTainted(node.getFalseExpression())) {
      fixes.addAll(
          node.getFalseExpression().accept(new FixVisitor(context, typeFactory, pair), null));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(NewClassTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (arg != null && typeFactory.mayBeTainted(arg)) {
        // Required can be null here, since we only need the passed parameters to be untainted.
        fixes.addAll(arg.accept(new FixVisitor(context, typeFactory, pair), unused));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (node.getExpression() != null && typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Void unused) {
    AnnotatedTypeMirror requiredComponentType =
        pair.required instanceof AnnotatedTypeMirror.AnnotatedArrayType
            ? ((AnnotatedTypeMirror.AnnotatedArrayType) pair.required).getComponentType()
            : null;
    AnnotatedTypeMirror foundComponentType =
        pair.found instanceof AnnotatedTypeMirror.AnnotatedArrayType
            ? ((AnnotatedTypeMirror.AnnotatedArrayType) pair.found).getComponentType()
            : null;
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      for (ExpressionTree arg : node.getInitializers()) {
        if (arg != null && typeFactory.mayBeTainted(arg)) {
          fixes.addAll(
              arg.accept(
                  new FixVisitor(
                      context,
                      typeFactory,
                      FoundRequired.of(foundComponentType, requiredComponentType)),
                  unused));
        }
      }
    }
    return fixes;
  }

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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getLeftOperand().accept(new FixVisitor(context, typeFactory, pair), null));
    fixes.addAll(node.getRightOperand().accept(new FixVisitor(context, typeFactory, pair), null));
    return fixes;
  }

  @Override
  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Void unused) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Void unused) {
    if (node.getExpression() != null && typeFactory.mayBeTainted(node.getExpression())) {
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    if (pair != null && pair.required != null && pair.found != null) {
      location.setTypeVariablePositions(
          new TypeMatchVisitor(typeFactory).visit(pair.found, pair.required, null));
    }
    return new Fix("untainted", location);
  }

  /**
   * Builds the location for the given element.
   *
   * @param element The element to build the location for.
   * @return The location for the given element.
   */
  protected SymbolLocation buildLocationForElement(Element element) {
    if (element == null) {
      return null;
    }
    return SymbolLocation.createLocationFromSymbol((Symbol) element, context);
  }
}

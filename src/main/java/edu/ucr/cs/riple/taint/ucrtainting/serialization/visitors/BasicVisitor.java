package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This visitor directly annotates the element declaration to match the required type. */
public class BasicVisitor extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /** The javac context. */
  protected final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  public BasicVisitor(Context context, UCRTaintingAnnotatedTypeFactory factory) {
    this.context = context;
    this.typeFactory = factory;
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
    if (requireFix(pair)) {
      fixes.addAll(node.getTrueExpression().accept(new FixVisitor(context, typeFactory), pair));
    }
    if (requireFix(pair)) {
      fixes.addAll(node.getFalseExpression().accept(new FixVisitor(context, typeFactory), pair));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(NewClassTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (requireFix(pair)) {
        // Required can be null here, since we only need the passed parameters to be untainted.
        fixes.addAll(arg.accept(new FixVisitor(context, typeFactory), pair));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    if (node.getExpression() != null && typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(new FixVisitor(context, typeFactory), pair));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, FoundRequired pair) {
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
                  new FixVisitor(context, typeFactory),
                  FoundRequired.of(foundComponentType, requiredComponentType)));
        }
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    if (calledMethod.getParameters().isEmpty()) {
      // no parameters, make untainted
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod, pair)));
    }
    JCTree decl = Utility.locateDeclaration(calledMethod, context);
    if (decl == null) {
      return Set.of();
    }
    return decl.accept(new MethodReturnVisitor(context, typeFactory), pair);
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory), pair);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(node.getLeftOperand().accept(new FixVisitor(context, typeFactory), pair));
    fixes.addAll(node.getRightOperand().accept(new FixVisitor(context, typeFactory), pair));
    return fixes;
  }

  @Override
  public Set<Fix> visitArrayAccess(ArrayAccessTree node, FoundRequired pair) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(new FixVisitor(context, typeFactory), pair);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
    if (requireFix(pair)) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }

      Fix fix = buildFixForElement(TreeUtils.elementFromUse(node), pair);
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitParenthesized(ParenthesizedTree node, FoundRequired pair) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory), pair);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, FoundRequired pair) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory), pair);
  }

  @Override
  public Set<Fix> visitCompoundAssignment(CompoundAssignmentTree node, FoundRequired pair) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory), pair);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    if (pair != null && pair.required != null && pair.found != null) {
      location.setTypeVariablePositions(
          new TypeMatchVisitor(typeFactory).visit(pair.found, pair.required, null));
    }
    return new Fix(location);
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

  /**
   * Checks if the fix is required.
   *
   * @return True if the fix is required, false otherwise.
   */
  protected boolean requireFix(FoundRequired pair) {
    if (pair == null || pair.found == null || pair.required == null) {
      return true;
    }
    AnnotatedTypeMirror widenedValueType = typeFactory.getWidenedType(pair.found, pair.required);
    return !typeFactory.getTypeHierarchy().isSubtype(widenedValueType, pair.required);
  }
}

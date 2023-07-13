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
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

  @Nullable protected final FoundRequired pair;

  public BasicVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    this.context = context;
    this.typeFactory = factory;
    this.pair = pair;
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
          node.getTrueExpression().accept(new FixVisitor(context, typeFactory, pair), null));
    }
    if (typeFactory.mayBeTainted(node.getFalseExpression())) {
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
      if (typeFactory.mayBeTainted(arg)) {
        // Required can be null here, since we only need the passed parameters to be untainted.
        fixes.addAll(arg.accept(new FixVisitor(context, typeFactory, null), unused));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Void unused) {
    AnnotatedTypeMirror required = pair == null ? null : pair.required;
    AnnotatedTypeMirror requiredComponentType =
        required instanceof AnnotatedTypeMirror.AnnotatedArrayType
            ? ((AnnotatedTypeMirror.AnnotatedArrayType) required).getComponentType()
            : null;
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      for (ExpressionTree arg : node.getInitializers()) {
        if (typeFactory.mayBeTainted(arg)) {
          fixes.addAll(
              arg.accept(
                  new FixVisitor(
                      context, typeFactory, FoundRequired.of(null, requiredComponentType)),
                  unused));
        }
      }
    }
    // Add a fix for each dimension.
    for (ExpressionTree arg : node.getDimensions()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(
            arg.accept(
                new FixVisitor(context, typeFactory, FoundRequired.of(null, requiredComponentType)),
                unused));
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Void unused) {
    AnnotatedTypeMirror required = pair == null ? null : pair.required;
    Set<Fix> fixes = new HashSet<>();
    // check if is logical and or operator ->
    JCTree.JCBinary binary = (JCTree.JCBinary) node;
    // check if binary is and tree
    AnnotatedTypeMirror binaryRequiredType = required;
    if (binary.getTag() == JCTree.Tag.AND || binary.getTag() == JCTree.Tag.OR) {
      // Here we only need both sides to be simple untainted.
      binaryRequiredType = null;
    }
    fixes.addAll(
        node.getLeftOperand()
            .accept(
                new FixVisitor(context, typeFactory, FoundRequired.of(null, binaryRequiredType)),
                null));
    fixes.addAll(
        node.getRightOperand()
            .accept(
                new FixVisitor(context, typeFactory, FoundRequired.of(null, binaryRequiredType)),
                null));
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Void unused) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, pair), unused);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Void unused) {
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
    if (pair != null) {
      location.setTypeVariablePositions(annotateType(pair.required, pair.found));
    }
    return new Fix("untainted", location);
  }

  protected List<List<Integer>> annotateType(
      AnnotatedTypeMirror required, AnnotatedTypeMirror found) {
    List<List<Integer>> list = new ArrayList<>();
    if (required instanceof AnnotatedTypeMirror.AnnotatedTypeVariable) {
      if (!typeFactory.hasUntaintedAnnotation(found)
          && typeFactory.hasUntaintedAnnotation(required)) {
        // e.g. @Untainted T
        list.add(List.of(0));
      }
      return list;
    }
    if (required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      // e.g. @Untainted String
      if (!typeFactory.hasUntaintedAnnotation(found)
          && typeFactory.hasUntaintedAnnotation(required)) {
        list.add(List.of(0));
      }
      AnnotatedTypeMirror.AnnotatedDeclaredType type =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) required;
      for (int i = 0; i < type.getTypeArguments().size(); i++) {
        AnnotatedTypeMirror typeArgumentFound =
            ((AnnotatedTypeMirror.AnnotatedDeclaredType) found).getTypeArguments().get(i);
        AnnotatedTypeMirror typeArgumentRequired = type.getTypeArguments().get(i);
        if (typeArgumentFound.equals(typeArgumentRequired)) {
          // We do not need to continue this branch.
          continue;
        }
        List<Integer> toAddOnThisTypeArg = new ArrayList<>();
        toAddOnThisTypeArg.add(i + 1);
        if (typeFactory.hasUntaintedAnnotation(typeArgumentRequired)
            && !typeFactory.hasUntaintedAnnotation(typeArgumentFound)) {
          // e.g. @Untainted List<@Untainted String>
          list.add(List.of(1 + i, 0));
        }
        List<List<Integer>> result = annotateType(typeArgumentRequired, typeArgumentFound);
        for (List<Integer> toAddOnContainingTypeArg : result) {
          // Need a fresh chain for each type.
          if (!toAddOnContainingTypeArg.isEmpty()) {
            List<Integer> toAddOnThisTypeArgWithContainingTypeArgs =
                new ArrayList<>(toAddOnThisTypeArg);
            toAddOnThisTypeArgWithContainingTypeArgs.addAll(toAddOnContainingTypeArg);
            list.add(toAddOnThisTypeArgWithContainingTypeArgs);
          }
        }
      }
      return list;
    }
    if (required instanceof AnnotatedTypeMirror.AnnotatedPrimitiveType) {
      if (!typeFactory.hasUntaintedAnnotation(found)
          && typeFactory.hasUntaintedAnnotation(required)) {
        // e.g. @Untainted int
        list.add(List.of(0));
      }
      return list;
    }
    if (required instanceof AnnotatedTypeMirror.AnnotatedWildcardType) {
      // e.g. @Untainted ? extends T
      AnnotatedTypeMirror.AnnotatedWildcardType wildcardRequired =
          (AnnotatedTypeMirror.AnnotatedWildcardType) required;
      AnnotatedTypeMirror.AnnotatedWildcardType wildcardFound =
          (AnnotatedTypeMirror.AnnotatedWildcardType) found;
      return annotateType(wildcardRequired.getExtendsBound(), wildcardFound.getExtendsBound());
    }
    return list;
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

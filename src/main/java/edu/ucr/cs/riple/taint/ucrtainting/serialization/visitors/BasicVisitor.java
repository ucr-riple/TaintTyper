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
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
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
          node.getTrueExpression().accept(new FixVisitor(context, typeFactory, required), null));
    }
    if (typeFactory.mayBeTainted(node.getFalseExpression())) {
      fixes.addAll(
          node.getFalseExpression().accept(new FixVisitor(context, typeFactory, required), null));
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
      fixes.addAll(
          node.getExpression().accept(new FixVisitor(context, typeFactory, required), unused));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Void unused) {
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
              arg.accept(new FixVisitor(context, typeFactory, requiredComponentType), unused));
        }
      }
    }
    // Add a fix for each dimension.
    for (ExpressionTree arg : node.getDimensions()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(
            arg.accept(new FixVisitor(context, typeFactory, requiredComponentType), unused));
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, required), unused);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Void unused) {
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
            .accept(new FixVisitor(context, typeFactory, binaryRequiredType), null));
    fixes.addAll(
        node.getRightOperand()
            .accept(new FixVisitor(context, typeFactory, binaryRequiredType), null));
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Void unused) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(new FixVisitor(context, typeFactory, required), unused);
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
    return node.getExpression().accept(new FixVisitor(context, typeFactory, required), unused);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Void unused) {
    return node.getExpression().accept(new FixVisitor(context, typeFactory, required), unused);
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
    if (required != null) {
      Type type = getType(element);
      location.setTypeVariablePositions(annotateType(type, required));
    }
    return new Fix("untainted", location);
  }

  protected List<List<Integer>> annotateType(Type type, AnnotatedTypeMirror required) {
    List<List<Integer>> list = new ArrayList<>();
    if (type instanceof Type.TypeVar
        && required instanceof AnnotatedTypeMirror.AnnotatedTypeVariable) {
      // e.g. @Untainted T
      list.add(List.of(0));
      return list;
    }
    if (type instanceof Type.ClassType
        && required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      // e.g. @Untainted String
      if (!Utility.hasUntaintedAnnotation(type) && typeFactory.hasUntaintedAnnotation(required)) {
        list.add(List.of(0));
      }
      for (int i = 0; i < type.getTypeArguments().size(); i++) {
        Type typeArgument = type.getTypeArguments().get(i);
        AnnotatedTypeMirror typeArgumentRequired =
            ((AnnotatedTypeMirror.AnnotatedDeclaredType) required).getTypeArguments().get(i);
        List<List<Integer>> result = annotateType(typeArgument, typeArgumentRequired);
        for (List<Integer> l : result) {
          List<Integer> newL = new ArrayList<>(l);
          newL.add(0, i + 1);
          list.add(newL);
        }
      }
      return list;
    }
    if (type instanceof Type.ArrayType
        && required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      // e.g. @Untainted String[]
      // Here we should annotate the component type
      annotateType(((Type.ArrayType) type).getComponentType(), required);
    }
    if (type instanceof Type.JCPrimitiveType) {
      // e.g. @Untainted int
      list.add(List.of(0));
      return list;
    }
    if (type instanceof Type.TypeVar
        && required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      // e.g. @Untainted T, should just annotate as @Untainted.
      list.add(List.of(0));
      return list;
    }
    return list;
  }

  /**
   * Gets the type of the given element. If the given element is a method, then the return type of
   * the method is returned.
   *
   * @param element The element to get the type for.
   * @return The type of the given element.
   */
  protected static Type getType(Element element) {
    return element instanceof Symbol.MethodSymbol
        ? ((Symbol.MethodSymbol) element).getReturnType()
        : ((Symbol) element).type;
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

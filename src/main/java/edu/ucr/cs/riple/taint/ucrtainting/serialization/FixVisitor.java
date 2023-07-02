package edu.ucr.cs.riple.taint.ucrtainting.serialization;

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
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Type> {

  /** The javac context. */
  private final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  private final UCRTaintingAnnotatedTypeFactory typeFactory;

  private final Tree errorTree;

  public FixVisitor(Context context, UCRTaintingAnnotatedTypeFactory factory, Tree errorTree) {
    this.context = context;
    this.typeFactory = factory;
    this.errorTree = errorTree;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Type typeVar) {
    if (typeFactory.mayBeTainted(node)) {
      Fix fix = buildFixForElement(TreeUtils.elementFromTree(node), typeVar);
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getTrueExpression())) {
      fixes.addAll(node.getTrueExpression().accept(this, typeVar));
    }
    if (typeFactory.mayBeTainted(node.getFalseExpression())) {
      fixes.addAll(node.getFalseExpression().accept(this, typeVar));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(com.sun.source.tree.NewClassTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(this, typeVar));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(this, typeVar));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      for (ExpressionTree arg : node.getInitializers()) {
        if (typeFactory.mayBeTainted(arg)) {
          fixes.addAll(arg.accept(this, typeVar));
        }
      }
    }
    // Add a fix for each dimension.
    for (ExpressionTree arg : node.getDimensions()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(this, typeVar));
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
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Type type) {
    if (typeFactory.mayBeTainted(node.getMethodSelect())) {
      Element element = TreeUtils.elementFromUse(node);
      if (element == null) {
        return Set.of();
      }
      Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
      // check if the call is to a method defined in a third party library.
      if (!Utility.isInAnnotatedPackage(calledMethod, typeFactory)) {
        // Check if the method is source defined in stubs.
        if (typeFactory.isFromStubFile(calledMethod)) {
          // We cannot do any fix here
          return Set.of();
        }
        // Check the return type, if it does not contain type variable, we can annotate the
        // receiver and all passed arguments.
        if ((!(calledMethod.getReturnType() instanceof Type.TypeVar))) {
          Set<Fix> fixes = new HashSet<>();
          Type finalType = type;
          // Add a fix for each passed argument.
          node.getArguments().forEach(arg -> fixes.addAll(arg.accept(this, finalType)));
          // Add the fix for the receiver if not static.
          if (calledMethod.isStatic()) {
            // No receiver for static method calls.
            return fixes;
          }
          // Build the fix for the receiver.
          fixes.addAll(
              ((MemberSelectTree) node.getMethodSelect()).getExpression().accept(this, type));
          return fixes;
        }
      }
      // check for type variable in return type. If present, we should annotate the declaration of
      // the receiver.
      if (Utility.containsTypeArgument(calledMethod.getReturnType())) {
        // set type, if not set.
        type = type == null ? calledMethod.getReturnType() : type;
        if (!calledMethod.isStatic() && node.getMethodSelect() instanceof MemberSelectTree) {
          ExpressionTree receiver = ((MemberSelectTree) node.getMethodSelect()).getExpression();
          if (!Utility.isThisIdentifier(receiver)) {
            // Build the fix for the receiver and leave the called method untouched. Annotation on
            // the declaration on the type argument, will be added on the method automatically.
            return receiver.accept(this, type);
          }
        }
        // Build the fix directly on the method symbol.
        return Set.of(Objects.requireNonNull(buildFixForElement(element, type)));
      } else {
        // Build a fix for the called method return type.
        return Set.of(
            Objects.requireNonNull(
                buildFixForElement(TreeUtils.elementFromTree(node.getMethodSelect()), null)));
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
    if (typeFactory.mayBeTainted(node.getExpression())) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }
      // If type is not null, we should annotate type parameter that matches the target type.
      if (type != null) {
        // Need to check if member contains a type argument that includes the type. In that case, we
        // found the right declaration.
        if (Utility.containsTypeArgument(((Symbol) member).type, type)) {
          Fix fix = buildFixForElement(TreeUtils.elementFromUse(node), type);
          return fix == null ? Set.of() : Set.of(fix);
        } else if (node instanceof JCTree.JCFieldAccess) {
          // Need to traverse the tree to find the right declaration.
          JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
          return fieldAccess.selected.accept(this, type);
        }
      } else {
        Fix fix = buildFixForElement(TreeUtils.elementFromUse(node), null);
        return fix == null ? Set.of() : Set.of(fix);
      }
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitParenthesized(ParenthesizedTree node, Type type) {
    return node.getExpression().accept(this, type);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Type type) {
    return node.getExpression().accept(this, type);
  }

  /**
   * Generates fixes for the given error tree.
   *
   * @return The set of fixes for the given error tree.
   */
  public Set<Fix> generateFixes() {
    return visit(errorTree, null);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @param type The type variable.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element, Type type) {
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

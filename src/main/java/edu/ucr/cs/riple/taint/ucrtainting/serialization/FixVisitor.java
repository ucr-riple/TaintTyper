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
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Type> {

  /** The javac context. */
  private final Context context;

  private final UCRTaintingAnnotatedTypeFactory typeFactory;

  public FixVisitor(Context context, UCRTaintingAnnotatedTypeFactory factory) {
    this.context = context;
    this.typeFactory = factory;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Type typeVar) {
    if (isTainted(node)) {
      return Set.of(buildFixForElement(TreeUtils.elementFromTree(node), typeVar));
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    if (isTainted(node.getTrueExpression())) {
      fixes.addAll(node.getTrueExpression().accept(this, typeVar));
    }
    if (isTainted(node.getFalseExpression())) {
      fixes.addAll(node.getFalseExpression().accept(this, typeVar));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(com.sun.source.tree.NewClassTree node, Type typeVar) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (isTainted(arg)) {
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
    if (isTainted(node.getMethodSelect())) {
      Element element = TreeUtils.elementFromUse(node);
      if (element == null) {
        return null;
      }
      Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
      // check if the call is to a method defined in third party library.
      if (calledMethod.enclClass().sourcefile == null
          || !Utility.isInAnnotatedPackage(calledMethod, typeFactory) && !calledMethod.isStatic()) {
        // Build the fix for the receiver.
        return ((MemberSelectTree) node.getMethodSelect()).getExpression().accept(this, type);
      }
      // check for type variable in return type.
      if (Utility.containsTypeParameter(calledMethod.getReturnType())) {
        // set type, if not set.
        type = type == null ? calledMethod.getReturnType() : type;
        if (node.getMethodSelect() instanceof MemberSelectTree) {
          ExpressionTree receiver = ((MemberSelectTree) node.getMethodSelect()).getExpression();
          if (!Utility.isThisIdentifier(receiver)) {
            // Build the fix for the receiver.
            return ((MemberSelectTree) node.getMethodSelect()).getExpression().accept(this, type);
          }
        }
        // Build the fix directly on the method symbol.
        return Set.of(buildFixForElement(element, type));
      } else {
        // Build a fix for the called method return type.
        return Set.of(buildFixForElement(TreeUtils.elementFromTree(node.getMethodSelect()), null));
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
    if (isTainted(node.getExpression())) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }
      if (type != null) {
        // Need to check if member affects the target type.
        if (!Utility.typeParameterDeterminedFromEncClass(((Symbol) member).enclClass(), type)) {
          return Set.of(buildFixForElement(TreeUtils.elementFromUse(node), type));
        } else if (node instanceof JCTree.JCFieldAccess) {
          JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
          return fieldAccess.selected.accept(this, type);
        }
      } else {
        return Set.of(buildFixForElement(TreeUtils.elementFromUse(node), null));
      }
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
   * @param element The element to build the fix for.
   * @param type The type variable.
   * @return The fix for the given element.
   */
  public Fix buildFixForElement(Element element, Type type) {
    SymbolLocation location;
    if (element == null) {
      return null;
    }
    location = SymbolLocation.createLocationFromSymbol((Symbol) element, context, type);
    return new Fix("untainted", location);
  }

  /**
   * Checks if the given tree is tainted.
   *
   * @param tree The given tree.
   * @return True if the given tree is tainted, false otherwise.
   */
  private boolean isTainted(Tree tree) {
    return typeFactory.getAnnotatedType(tree).hasAnnotation(RTainted.class);
  }
}

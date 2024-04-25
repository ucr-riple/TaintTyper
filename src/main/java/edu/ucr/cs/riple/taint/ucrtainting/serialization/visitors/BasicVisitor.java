package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CollectionHandler;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This visitor directly annotates the element declaration to match the required type. */
public class BasicVisitor extends SpecializedFixComputer {

  protected final MethodReturnVisitor returnVisitor;
  protected TreePath currentPath;
  protected final Types types;

  public BasicVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    this.returnVisitor = new MethodReturnVisitor(typeFactory, fixComputer, context);
    this.types = Types.instance(context);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    if (requireFix(pair)) {
      Fix fix = buildFixForElement(element, pair);
      if (fix == null) {
        // TODO Hacky , fix later.
        // Check if the created method has the parameter with the same name as the identifier.
        // We might be inside a lambda expression, so we need to check the parameters of the method
        if (!(element instanceof Symbol.VarSymbol)) {
          return Set.of();
        }
        Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
        currentPath = TreePath.getPath(currentPath, node);
        while (currentPath != null
            && !currentPath.getLeaf().getKind().equals(Tree.Kind.LAMBDA_EXPRESSION)
            && !currentPath.getLeaf().getKind().equals(Tree.Kind.METHOD)) {
          currentPath = currentPath.getParentPath();
          if (currentPath == null || currentPath.getLeaf() == null) {
            return Set.of();
          }
        }
        if (currentPath == null) {
          return Set.of();
        }
        if (currentPath.getLeaf().getKind().equals(Tree.Kind.LAMBDA_EXPRESSION)) {
          LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) currentPath.getLeaf();
          int index = 0;
          for (VariableTree variableTree : lambdaExpressionTree.getParameters()) {
            if (varSymbol.getSimpleName().equals(variableTree.getName())) {
              Symbol.MethodSymbol methodSymbol =
                  SymbolUtils.getFunctionalInterfaceMethod(
                      lambdaExpressionTree, Types.instance(context));
              if (methodSymbol == null) {
                return Set.of();
              }
              if (index >= methodSymbol.getParameters().size()) {
                return Set.of();
              }
              Fix onSuperMethodParameter =
                  buildFixForElement(methodSymbol.getParameters().get(index), pair);
              return onSuperMethodParameter == null ? Set.of() : Set.of(onSuperMethodParameter);
            }
            index++;
          }
        }
      } else {
        // check if node is of type Class<?>
        if (((Symbol.VarSymbol) element)
            .type
            .tsym
            .getQualifiedName()
            .toString()
            .equals("java.lang.Class")) {
          // We cannot annotate Class<?> as @Untainted or as Class<@RUntainted ?>
          if (fix.location.getTypeIndexSet().equals(TypeIndex.setOf(1, 0))) {
            return Set.of();
          }
        }
        return Set.of(fix);
      }
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
    if (!typeFactory.hasUntaintedAnnotation(pair.found)
        && typeFactory.hasUntaintedAnnotation(pair.required)) {
      // Add a fix for each argument.
      for (ExpressionTree arg : node.getArguments()) {
        AnnotatedTypeMirror foundArgType = typeFactory.getAnnotatedType(arg);
        AnnotatedTypeMirror requiredArgType = foundArgType.deepCopy(true);
        if (requiredArgType instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
          typeFactory.makeUntainted(
              ((AnnotatedTypeMirror.AnnotatedArrayType) requiredArgType).getComponentType());
        } else {
          typeFactory.makeUntainted(requiredArgType);
        }
        fixes.addAll(
            arg.accept(fixComputer, new FoundRequired(foundArgType, requiredArgType, pair.depth)));
      }
    }
    if (CollectionHandler.implementsCollectionInterface(
            (Type) pair.required.getUnderlyingType(), types)
        && CollectionHandler.implementsCollectionInterface(
            (Type) pair.found.getUnderlyingType(), types)) {
      AnnotatedTypeMirror.AnnotatedDeclaredType foundType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.found;
      AnnotatedTypeMirror.AnnotatedDeclaredType requiredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.required;
      if (typeFactory.hasUntaintedAnnotation(requiredType.getTypeArguments().get(0))
          && !typeFactory.hasUntaintedAnnotation(foundType.getTypeArguments().get(0))) {
        if (node.getArguments().size() == 1) {
          ExpressionTree arg = node.getArguments().get(0);
          AnnotatedTypeMirror foundArgType = typeFactory.getAnnotatedType(arg);
          if (foundArgType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
            AnnotatedTypeMirror.AnnotatedDeclaredType foundArgDeclaredType =
                (AnnotatedTypeMirror.AnnotatedDeclaredType) foundArgType;
            AnnotatedTypeMirror.AnnotatedDeclaredType requiredArgType =
                foundArgDeclaredType.deepCopy(true);
            typeFactory.makeUntainted(requiredArgType.getTypeArguments().get(0));
            fixes.addAll(
                arg.accept(
                    fixComputer, new FoundRequired(foundArgType, requiredArgType, pair.depth)));
          }
        }
      }
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
    if (pair.found instanceof AnnotatedTypeMirror.AnnotatedArrayType
        && pair.required instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      AnnotatedTypeMirror.AnnotatedArrayType foundArrayType =
          (AnnotatedTypeMirror.AnnotatedArrayType) pair.found;
      AnnotatedTypeMirror.AnnotatedArrayType requiredArrayType =
          (AnnotatedTypeMirror.AnnotatedArrayType) pair.required;
      FoundRequired newPair =
          new FoundRequired(
              foundArrayType.getComponentType(), requiredArrayType.getComponentType(), pair.depth);
      // Add a fix for each argument.
      if (node.getInitializers() != null) {
        node.getInitializers().forEach(arg -> fixes.addAll(arg.accept(fixComputer, newPair)));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    if (typeFactory.isFromStubFile(calledMethod)) {
      return Set.of();
    }
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
    JCTree decl = SymbolUtils.locateDeclaration(calledMethod, context);
    if (decl == null || decl.getKind() != Tree.Kind.METHOD) {
      return Set.of(onMethod);
    }
    JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) decl;
    if (methodDecl.getBody() == null) {
      return Set.of(onMethod);
    }
    if (!typeFactory.polyTaintInferenceEnabled()) {
      return Set.of(onMethod);
    }
    returnVisitor.addInvocation(node, pair);
    return new HashSet<>(methodDecl.accept(returnVisitor, pair));
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

  public void reset(TreePath currentPath) {
    this.returnVisitor.reset();
    this.currentPath = currentPath;
  }
}

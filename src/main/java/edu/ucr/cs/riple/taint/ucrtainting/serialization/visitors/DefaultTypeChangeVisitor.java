/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This visitor directly annotates the element declaration to match the required type. */
public class DefaultTypeChangeVisitor extends SpecializedFixComputer {

  /** The visitor that accumulates the results of visiting a return statements in a method. */
  protected final MethodReturnVisitor returnVisitor;
  /** Current path of the CF visitor. */
  protected TreePath currentPath;
  /** Javac types instance. */
  protected final Types types;
  /** Visitor that fixes all assignments on local variables. */
  protected final LocalVariableFixVisitor localVariableFixVisitor;

  public DefaultTypeChangeVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    this.returnVisitor = new MethodReturnVisitor(typeFactory, fixComputer, context);
    this.types = Types.instance(context);
    this.localVariableFixVisitor = new LocalVariableFixVisitor(typeFactory, fixComputer, context);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    if (requireFix(pair)) {
      Fix fix = buildFixForElement(element, pair);
      if (fix == null) {
        // if fix is null, it might be possible that the target is a parameter in a lambda
        // expression.
        if (!(element instanceof Symbol.VarSymbol)) {
          return Set.of();
        }
        Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
        if (!varSymbol.getKind().equals(ElementKind.PARAMETER)) {
          return Set.of();
        }
        // Target is a parameter, we have to locate the overridden method and the parameter index.
        // update path to the current path
        currentPath = TreePath.getPath(currentPath, node);
        LambdaExpressionTree lambdaExpressionTree =
            SymbolUtils.findEnclosingNode(currentPath, LambdaExpressionTree.class);
        if (lambdaExpressionTree == null) {
          return Set.of();
        }
        Symbol.MethodSymbol overriddenMethod =
            SymbolUtils.getFunctionalInterfaceMethod(lambdaExpressionTree, types);
        if (overriddenMethod == null) {
          return Set.of();
        }
        List<Name> parameterNames =
            lambdaExpressionTree.getParameters().stream()
                .map(VariableTree::getName)
                .collect(Collectors.toList());
        int index = parameterNames.indexOf(varSymbol.getSimpleName());
        if (index == -1) {
          return Set.of();
        }
        fix = buildFixForElement(overriddenMethod.getParameters().get(index), pair);
      }
      if (fix == null) {
        return Set.of();
      }
      // check if node is of type Class<?>
      if (TypeUtils.getType(fix.location.getTarget())
          .tsym
          .getQualifiedName()
          .toString()
          .equals("java.lang.Class")) {
        // We cannot annotate Class<?> as Class<@RUntainted ?>
        if (fix.location.getTypeIndexSet().equals(TypeIndex.setOf(1, 0))) {
          return Set.of();
        }
      }
      return fix.location.getKind().isLocalVariable()
          ? localVariableFixVisitor.visitIdentifier(node, pair)
          : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, FoundRequired pair) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(
        node.getTrueExpression()
            .accept(
                fixComputer, typeFactory.makeUntaintedPair(node.getTrueExpression(), pair.depth)));
    fixes.addAll(
        node.getFalseExpression()
            .accept(
                fixComputer, typeFactory.makeUntaintedPair(node.getFalseExpression(), pair.depth)));
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(NewClassTree node, FoundRequired pair) {
    if (!typeFactory.hasUntaintedAnnotation(pair.found)
        && typeFactory.hasUntaintedAnnotation(pair.required)) {
      // Add a fix for each argument.
      Set<Fix> onArguments = new HashSet<>();
      for (ExpressionTree arg : node.getArguments()) {
        AnnotatedTypeMirror foundArgType = typeFactory.getAnnotatedType(arg);
        AnnotatedTypeMirror requiredArgType = foundArgType.deepCopy(true);
        if (requiredArgType instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
          typeFactory.makeUntainted(
              ((AnnotatedTypeMirror.AnnotatedArrayType) requiredArgType).getComponentType());
        } else {
          typeFactory.makeUntainted(requiredArgType);
        }
        onArguments.addAll(
            arg.accept(fixComputer, new FoundRequired(foundArgType, requiredArgType, pair.depth)));
      }
      return onArguments;
    }
    return Set.of();
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
    FoundRequired leftPair;
    FoundRequired rightPair;
    JCTree.Tag tag = ((JCTree.JCBinary) node).getTag();
    if (tag == JCTree.Tag.EQ || tag == JCTree.Tag.NE) {
      leftPair = typeFactory.makeUntaintedPair(left, pair.depth);
      rightPair = typeFactory.makeUntaintedPair(right, pair.depth);
    } else {
      leftPair = FoundRequired.of(typeFactory.getAnnotatedType(left), pair.required, pair.depth);
      rightPair = FoundRequired.of(typeFactory.getAnnotatedType(right), pair.required, pair.depth);
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
   * Checks if any annotation is required to adapt the found type to the required type.
   *
   * @return True if a fix is required, false otherwise.
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

  /**
   * Resets the visitor state to be used for a new fix computation.
   *
   * @param currentPath The current path of the CF visitor.
   */
  public void reset(TreePath currentPath) {
    this.returnVisitor.reset();
    this.currentPath = currentPath;
    this.localVariableFixVisitor.reset();
  }
}

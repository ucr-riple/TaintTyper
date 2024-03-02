package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import java.util.List;
import javax.lang.model.element.ElementKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

public class UCRTaintingTreeAnnotator extends TreeAnnotator {

  private final Handler handler;
  private final UCRTaintingAnnotatedTypeFactory typeFactory;

  /**
   * UCRTaintingTreeAnnotator
   *
   * @param typeFactory the type factory
   */
  protected UCRTaintingTreeAnnotator(UCRTaintingAnnotatedTypeFactory typeFactory, Handler handler) {
    super(typeFactory);
    this.typeFactory = typeFactory;
    this.handler = handler;
  }

  /**
   * Visits all method invocations and updates {@link AnnotatedTypeMirror} according to the argument
   * and receiver annotations. If any of the arguments or the receiver is {@link RTainted}, the
   * {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated return type of the method invocation
   */
  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    handler.visitMethodInvocation(node, annotatedTypeMirror);
    return super.visitMethodInvocation(node, annotatedTypeMirror);
  }

  @Override
  public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    handler.visitVariable(node, annotatedTypeMirror);
    return super.visitVariable(node, annotatedTypeMirror);
  }

  @Override
  public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    typeFactory.makeUntainted(annotatedTypeMirror);
    return super.visitLiteral(node, annotatedTypeMirror);
  }

  /**
   * Visits member select trees and updates {@link AnnotatedTypeMirror} according to the identifier.
   * Particularly, it applies the type of the identifier in the expression to the whole expression.
   * E.g. for expression {@code e.g.h.b}, if {@code b} is untainted, then {@code e.g.h.b} is
   * untainted.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated return type of the member select
   */
  @Override
  public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    Symbol symbol = (Symbol) TreeUtils.elementFromUse(node);
    if (symbol.getKind().equals(ElementKind.PACKAGE)) {
      typeFactory.makeUntainted(annotatedTypeMirror);
      return super.visitMemberSelect(node, annotatedTypeMirror);
    }
    if (symbol.getKind().isField() && node instanceof JCTree.JCFieldAccess) {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
      if (typeFactory.isThirdPartyField((Symbol.VarSymbol) symbol)
          && !typeFactory.hasTaintedReceiver(fieldAccess)) {
        typeFactory.makeDeepUntainted(annotatedTypeMirror);
        return super.visitMemberSelect(node, annotatedTypeMirror);
      }
    }
    // make .class untainted
    if (node.getIdentifier().toString().equals("class")
        && annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      typeFactory.makeUntainted(annotatedTypeMirror);
    }
    return super.visitMemberSelect(node, annotatedTypeMirror);
  }

  /**
   * Visits all new class creations and updates {@link AnnotatedTypeMirror} according to the
   * argument and receiver annotations. If any of the arguments or the receiver is {@link RTainted},
   * the {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated type of the new class
   */
  @Override
  public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (!typeFactory.hasTaintedArgument(node)) {
      typeFactory.makeUntainted(annotatedTypeMirror);
    }
    return super.visitNewClass(node, annotatedTypeMirror);
  }

  @Override
  public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror mirror) {
    List<? extends ExpressionTree> initializers = node.getInitializers();
    boolean allUntainted = true;
    if (initializers != null) {
      for (ExpressionTree initializer : initializers) {
        if (initializer != null && typeFactory.mayBeTainted(initializer)) {
          allUntainted = false;
          break;
        }
      }
    }
    if (allUntainted) {
      typeFactory.makeUntainted(mirror);
    }
    return super.visitNewArray(node, mirror);
  }

  @Override
  public Void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    typeFactory.makeUntainted(annotatedTypeMirror);
    return super.visitLambdaExpression(node, annotatedTypeMirror);
  }
}

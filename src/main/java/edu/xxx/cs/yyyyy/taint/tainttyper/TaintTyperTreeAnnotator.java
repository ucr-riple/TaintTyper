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

package edu.xxx.cs.yyyyy.taint.tainttyper;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.xxx.cs.yyyyy.taint.tainttyper.handlers.Handler;
import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RTainted;
import edu.xxx.cs.yyyyy.taint.tainttyper.util.SymbolUtils;
import java.util.List;
import javax.lang.model.element.ElementKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

public class TaintTyperTreeAnnotator extends TreeAnnotator {

  private final Handler handler;
  private final TaintTyperAnnotatedTypeFactory typeFactory;

  /**
   * TaintTyperTreeAnnotator
   *
   * @param typeFactory the type factory
   * @param handler Handler to apply custom behavior
   */
  protected TaintTyperTreeAnnotator(TaintTyperAnnotatedTypeFactory typeFactory, Handler handler) {
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
    // Make getClass() and this.getClass() untainted
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    boolean hasThisReceiver = receiver == null || SymbolUtils.isThisIdentifier(receiver);
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    if (methodSymbol.getSimpleName().contentEquals("getClass") && hasThisReceiver) {
      typeFactory.makeUntainted(annotatedTypeMirror);
    }
    if (methodSymbol.owner instanceof Symbol.ClassSymbol) {
      Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) methodSymbol.owner;
      if (classSymbol.isAnnotationType()) {
        typeFactory.makeUntainted(annotatedTypeMirror);
        if (annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
          typeFactory.makeUntainted(
              ((AnnotatedTypeMirror.AnnotatedArrayType) annotatedTypeMirror).getComponentType());
        }
      }
    }
    handler.visitMethodInvocation(node, annotatedTypeMirror);
    return super.visitMethodInvocation(node, annotatedTypeMirror);
  }

  @Override
  public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) node;
    if (variableDecl.sym.type instanceof Type.ArrayType) {
      if (((Type.ArrayType) variableDecl.sym.type).isVarargs()) {
        // Arrays reference is always untainted
        typeFactory.makeUntainted(annotatedTypeMirror);
      }
    }
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
      if (typeFactory.isUnannotatedField((Symbol.VarSymbol) symbol)
          && !typeFactory.hasTaintedReceiver(fieldAccess)) {
        typeFactory.makeUntainted(annotatedTypeMirror);
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
    handler.visitNewClass(node, annotatedTypeMirror);
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
    handler.visitLambdaExpression(node, annotatedTypeMirror);
    return super.visitLambdaExpression(node, annotatedTypeMirror);
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (!typeFactory.mayBeTainted(node.getExpression())) {
      typeFactory.makeUntainted(annotatedTypeMirror);
    }
    return super.visitTypeCast(node, annotatedTypeMirror);
  }
}

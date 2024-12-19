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

package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class CompositHandler implements Handler {

  /** Set of handlers to be used to add annotations from default for type. */
  private final ImmutableSet<Handler> handlers;

  public CompositHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    ImmutableSet.Builder<Handler> handlerBuilder = new ImmutableSet.Builder<>();
    handlerBuilder.add(new StaticFinalFieldHandler(typeFactory));
    handlerBuilder.add(new EnumHandler(typeFactory));
    if (typeFactory.unannotatedCodeHandlingEnabled()) {
      handlerBuilder.add(new UnannotatedCodeHandler(typeFactory, context));
    }
    handlerBuilder.add(new CollectionHandler(typeFactory, context));
    handlerBuilder.add(new AnnotationMemberHandler(typeFactory));
    handlerBuilder.add(new SanitizerHandler(typeFactory));
    handlerBuilder.add(new LambdaHandler(typeFactory, context));
    this.handlers = handlerBuilder.build();
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.addAnnotationsFromDefaultForType(element, type));
  }

  @Override
  public void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitVariable(variableTree, type));
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitMethodInvocation(tree, type));
  }

  @Override
  public void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitMemberSelect(tree, type));
  }

  @Override
  public void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitNewClass(tree, type));
  }

  @Override
  public void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    this.handlers.forEach(handler -> handler.visitLambdaExpression(node, annotatedTypeMirror));
  }
}

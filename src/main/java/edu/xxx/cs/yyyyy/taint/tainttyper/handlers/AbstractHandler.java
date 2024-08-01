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

package edu.xxx.cs.yyyyy.taint.tainttyper.handlers;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import edu.xxx.cs.yyyyy.taint.tainttyper.TaintTyperAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public abstract class AbstractHandler implements Handler {

  protected final TaintTyperAnnotatedTypeFactory typeFactory;

  public AbstractHandler(TaintTyperAnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    // no-op
  }
}

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
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Interface for handlers that add annotations from default for type and visit different types of
 * trees and may modify the type of the tree.
 */
public interface Handler {

  /**
   * Adds annotations from default for the given element and type.
   *
   * @param element The element to add annotations from default for.
   * @param type The type to add annotations from default for.
   */
  void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type);

  /**
   * Visits the given variable tree and may modify the type of the variable tree.
   *
   * @param variableTree The variable tree to visit.
   * @param type The type of the variable tree.
   */
  void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type);

  /**
   * Visits the given method invocation tree and may modify the type of the method invocation tree.
   *
   * @param tree The method invocation tree to visit.
   * @param type The type of the method invocation tree.
   */
  void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given member select tree and may modify the type of the member select tree.
   *
   * @param tree The member select tree to visit.
   * @param type The type of the member select tree.
   */
  void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given new class tree and may modify the type of the new class tree.
   *
   * @param tree The new class tree to visit.
   * @param type The type of the new class tree.
   */
  void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given lambda expression tree and may modify the type of the lambda expression tree.
   *
   * @param node The lambda expression tree to visit.
   * @param annotatedTypeMirror The type of the lambda expression tree.
   */
  void visitLambdaExpression(LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror);
}

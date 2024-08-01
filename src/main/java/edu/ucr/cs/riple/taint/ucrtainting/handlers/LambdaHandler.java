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

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.TaintTyperAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Handler for lambda expressions. Lambda expressions parameters are considered untainted if the
 * overridden method is from unannotated code.
 */
public class LambdaHandler extends AbstractHandler {

  /**
   * Set of lambda parameters that are visited previously by this handler. Parameters added to this
   * handler will be considered as untainted if the overridden method is from unannotated code.
   */
  private final Set<Symbol.VarSymbol> lambdaParameters;

  private final Types types;

  public LambdaHandler(TaintTyperAnnotatedTypeFactory typeFactory, Context context) {
    super(typeFactory);
    this.lambdaParameters = new HashSet<>();
    this.types = Types.instance(context);
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    if (element instanceof Symbol.VarSymbol && lambdaParameters.contains(element)) {
      typeFactory.makeUntainted(type);
    }
  }

  @Override
  public void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    typeFactory.makeUntainted(annotatedTypeMirror);
    Symbol.MethodSymbol overriddenMethod = SymbolUtils.getFunctionalInterfaceMethod(node, types);
    if (overriddenMethod != null && typeFactory.isUnannotatedMethod(overriddenMethod)) {
      node.getParameters()
          .forEach(
              variableTree ->
                  this.lambdaParameters.add(
                      (Symbol.VarSymbol) TreeUtils.elementFromDeclaration(variableTree)));
    }
    super.visitLambdaExpression(node, annotatedTypeMirror);
  }
}

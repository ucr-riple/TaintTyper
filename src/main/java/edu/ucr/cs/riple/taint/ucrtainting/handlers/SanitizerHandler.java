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

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.TaintTyperAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This handler is responsible for making the invocation of stub methods untainted if the invoked
 * method is annotated as untainted. Note: not sure why this is necessary, as the annotations in the
 * stubs should be enough to determine the taint of the method.
 */
public class SanitizerHandler extends AbstractHandler {

  public SanitizerHandler(TaintTyperAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    Element element = TreeUtils.elementFromUse(tree);
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
    if (!typeFactory.isFromStubFile(methodSymbol)) {
      return;
    }
    AnnotatedTypeMirror annotatedTypeMirror =
        typeFactory.stubTypes.getAnnotatedTypeMirror(methodSymbol);
    if (!(annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedExecutableType)) {
      return;
    }
    AnnotatedTypeMirror.AnnotatedExecutableType executableType =
        (AnnotatedTypeMirror.AnnotatedExecutableType) annotatedTypeMirror;
    if (typeFactory.hasUntaintedAnnotation(executableType.getReturnType())) {
      typeFactory.makeUntainted(type);
    }
    super.visitMethodInvocation(tree, type);
  }
}

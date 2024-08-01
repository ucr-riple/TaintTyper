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

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;

public class TaintTyperTypeAnnotator extends DefaultForTypeAnnotator {

  TaintTyperAnnotatedTypeFactory factory;

  /**
   * Creates a new TypeAnnotator.
   *
   * @param atypeFactory the type factory
   */
  protected TaintTyperTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
    super(atypeFactory);
    this.factory = (TaintTyperAnnotatedTypeFactory) atypeFactory;
  }

  @Override
  public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void unused) {
    return super.visitDeclared(type, unused);
  }

  @Override
  public Void visitArray(AnnotatedTypeMirror.AnnotatedArrayType type, Void unused) {
    factory.makeUntainted(type);
    return super.visitArray(type, unused);
  }
}

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

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors;

import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.ClassDeclarationLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.FieldLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.LocalVariableLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.MethodLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.MethodParameterLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.PolyMethodLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.SymbolLocation;

/**
 * A visitor of types, in the style of the visitor design pattern. When a visitor is passed to a
 * location's {@link SymbolLocation#accept accept} method, the <code>visit<i>Xyz</i></code> method
 * applicable to that location is invoked.
 */
public interface LocationVisitor<R, P> {

  /**
   * Visits a location for a method.
   *
   * @param method the location for a method
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitMethod(MethodLocation method, P p);

  /**
   * Visits a location for a field.
   *
   * @param field the location for a field
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitField(FieldLocation field, P p);

  /**
   * Visits a location for a parameter.
   *
   * @param parameter the location for a parameter
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitParameter(MethodParameterLocation parameter, P p);

  /**
   * Visits a location for a local variable.
   *
   * @param localVariable the location for a local variable
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitLocalVariable(LocalVariableLocation localVariable, P p);

  /**
   * Visits a location for a polymorphic method.
   *
   * @param polyMethodLocation the location for a polymorphic method
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitPolyMethod(PolyMethodLocation polyMethodLocation, P p);

  /**
   * Visits a location for a class declaration.
   *
   * @param classDeclarationLocation the location for a class declaration.
   * @param p a visitor-specified parameter
   * @return a visitor-specified result
   */
  R visitClassDeclaration(ClassDeclarationLocation classDeclarationLocation, P p);
}

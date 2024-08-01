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

package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.org.plumelib.util.ArraysPlume;

/** Visitor for the {@link TaintTyperChecker}. */
public class TaintTyperVisitor extends AccumulationVisitor {

  /**
   * Creates a {@link TaintTyperVisitor}.
   *
   * @param checker the checker that uses this visitor
   */
  public TaintTyperVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Don't check that the constructor result is top. Checking that the super() or this() call is a
   * subtype of the constructor result is sufficient.
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    commonAssignmentCheckStartDiagnostic(varType, valueType, valueTree);

    AnnotatedTypeMirror widenedValueType = atypeFactory.getWidenedType(valueType, varType);
    boolean success = atypeFactory.getTypeHierarchy().isSubtype(widenedValueType, varType);
    // Use an error key only if it's overridden by a checker.
    if (!success) {
      FoundRequired pair = FoundRequired.of(valueType, varType, 0);
      String valueTypeString = pair.foundString;
      String varTypeString = pair.requiredString;
      ((TaintTyperChecker) checker)
          .detailedReportError(
              valueTree,
              errorKey,
              pair,
              ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString));
    }
    return success;
  }

  public AnnotatedExecutableType getAnnotatedTypeOfOverriddenMethod(
      ExecutableElement methodElement) {
    AnnotatedExecutableType firsAnswer = null;
    // Find which methods this method overrides
    Map<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
        AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
    for (Map.Entry<AnnotatedTypeMirror.AnnotatedDeclaredType, ExecutableElement> pair :
        overriddenMethods.entrySet()) {
      AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType = pair.getKey();
      ExecutableElement overriddenMethodElt = pair.getValue();
      AnnotatedExecutableType current =
          AnnotatedTypes.asMemberOf(types, atypeFactory, overriddenType, overriddenMethodElt);
      if (firsAnswer == null) {
        firsAnswer = current;
      }
      if (((Symbol.MethodSymbol) overriddenMethodElt).enclClass().sourcefile != null) {
        return current;
      }
    }
    return firsAnswer;
  }
}

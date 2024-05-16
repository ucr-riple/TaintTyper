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

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class PolyMethodVisitor extends SpecializedFixComputer {

  public PolyMethodVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    super(typeFactory, fixComputer, context);
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    AnnotatedTypeMirror returnTypeCopy =
        typeFactory.getAnnotatedType(calledMethod).getReturnType().deepCopy(true);
    typeFactory.replacePolyWithUntainted(returnTypeCopy);
    AnnotatedTypeMirror widenedValueType =
        typeFactory.getWidenedType(returnTypeCopy, pair.required);
    if (typeFactory.getTypeHierarchy().isSubtype(widenedValueType, pair.required)) {
      // we should add a fix for each poly tainted argument
      Set<Fix> onArguments = new HashSet<>();
      for (int i = 0; i < node.getArguments().size(); i++) {
        AnnotatedTypeMirror argType = typeFactory.getAnnotatedType(node.getArguments().get(i));
        AnnotatedTypeMirror formalParameterAnnotatedTypeMirror =
            typeFactory.getAnnotatedType(calledMethod.getParameters().get(i));
        // Check if the formal parameter is poly tainted.
        if (!typeFactory.hasPolyTaintedAnnotation(formalParameterAnnotatedTypeMirror)) {
          continue;
        }
        AnnotatedTypeMirror copyArg = argType.deepCopy(true);
        typeFactory.replacePolyWithUntainted(copyArg, formalParameterAnnotatedTypeMirror);
        onArguments.addAll(
            node.getArguments()
                .get(i)
                .accept(fixComputer, FoundRequired.of(argType, copyArg, pair.depth)));
      }
      return onArguments;
    }
    return Set.of();
  }
}

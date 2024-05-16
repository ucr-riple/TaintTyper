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

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Visitor for handling calls to unannotated code. This visitor gets to the required type by making
 * receiver and all arguments be untainted.
 */
public class UnannotatedCodeFixVisitor extends SpecializedFixComputer {

  private final boolean activation;

  public UnannotatedCodeFixVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    this.activation = factory.unannotatedCodeHandlingEnabled();
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
    if (!activation) {
      return Set.of();
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    if (receiver == null) {
      return Set.of();
    }
    Element field = TreeUtils.elementFromUse(node);
    if (field == null || ElementUtils.isStatic(field)) {
      return Set.of();
    }
    return receiver.accept(fixComputer, typeFactory.makeUntaintedPair(receiver, pair.depth));
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    if (!activation) {
      return Set.of();
    }
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    // check if the call is to a method defined in unannotated code.
    if (!typeFactory.isUnannotatedMethod(calledMethod)) {
      return Set.of();
    }
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each passed argument.
    for (int i = 0; i < node.getArguments().size(); i++) {
      ExpressionTree argument = node.getArguments().get(i);
      AnnotatedTypeMirror formalParameterType =
          typeFactory.getAnnotatedType(node.getArguments().get(i)).deepCopy(true);
      makeUntainted(formalParameterType, typeFactory);
      AnnotatedTypeMirror actualParameterType = typeFactory.getAnnotatedType(argument);
      FoundRequired argFoundRequired = null;
      // check for varargs of called method, if the formal parameter is an array and the actual is
      // only a single element, we should match with the component type.
      if (i >= calledMethod.getParameters().size() - 1 && calledMethod.isVarArgs()) {
        if (formalParameterType instanceof AnnotatedTypeMirror.AnnotatedArrayType
            && !(actualParameterType instanceof AnnotatedTypeMirror.AnnotatedArrayType)) {
          argFoundRequired =
              FoundRequired.of(
                  actualParameterType,
                  ((AnnotatedTypeMirror.AnnotatedArrayType) formalParameterType).getComponentType(),
                  pair.depth);
        }
      }
      argFoundRequired =
          argFoundRequired == null
              ? FoundRequired.of(actualParameterType, formalParameterType, pair.depth)
              : argFoundRequired;
      fixes.addAll(argument.accept(fixComputer, argFoundRequired));
    }
    // Add the fix for the receiver if not static.
    if (calledMethod.isStatic()) {
      // No receiver for static method calls.
      return fixes;
    }
    // Build the fix for the receiver.
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    if (receiver != null) {
      fixes.addAll(
          receiver.accept(fixComputer, typeFactory.makeUntaintedPair(receiver, pair.depth)));
    }
    return fixes;
  }

  /**
   * Makes the given type untainted. If the type is an array, the component type is made untainted.
   * Do not use or change {@link UCRTaintingAnnotatedTypeFactory#makeUntainted} method.
   *
   * @param type The type to make untainted.
   * @param factory The type factory.
   */
  private void makeUntainted(AnnotatedTypeMirror type, UCRTaintingAnnotatedTypeFactory factory) {
    // for arrays, we need to pass collection of untainted data rather than tainted.
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      ((AnnotatedTypeMirror.AnnotatedArrayType) type)
          .getComponentType()
          .replaceAnnotation(factory.rUntainted);
    } else {
      type.replaceAnnotation(factory.rUntainted);
    }
  }
}

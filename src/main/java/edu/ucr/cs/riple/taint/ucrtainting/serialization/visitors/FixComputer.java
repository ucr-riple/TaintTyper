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
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * General Fix visitor.This visitor determines the approach for resolving the error upon visiting
 * specific nodes that may impact the algorithm selection.
 */
public class FixComputer extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  protected final Types types;
  protected final Context context;
  protected final DefaultTypeChangeVisitor defaultTypeChangeVisitor;
  protected final SpecializedFixComputer methodTypeArgumentFixVisitor;

  public FixComputer(UCRTaintingAnnotatedTypeFactory factory, Types types, Context context) {
    this.typeFactory = factory;
    this.context = context;
    this.types = types;
    this.defaultTypeChangeVisitor = new DefaultTypeChangeVisitor(factory, this, context);
    this.methodTypeArgumentFixVisitor = new GenericMethodFixVisitor(typeFactory, this, context);
  }

  @Override
  public Set<Fix> defaultAction(Tree node, FoundRequired pair) {
    return answer(node.accept(defaultTypeChangeVisitor, pair));
  }

  /**
   * Visitor for method invocations. In method invocations, we might choose different approaches:
   *
   * <ol>
   *   <li>If in stub files, exit
   *   <li>If method has type args, and by changing the parameter types of parameters, we can
   *       achieve required type, we annotate the parameters.
   *   <li>If return type of method has type arguments and the call has a valid receiver, we
   *       annotate the receiver.
   *   <li>If method is in third party library, we annotate the receiver and parameters.
   *   <li>Annotate the called method directly
   * </ol>
   *
   * @param node The given tree.
   * @return Void null.
   */
  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Element element;
    try {
      // It has been observed that in some cases, CF crashes in finding the element, in such cases
      // we should just return an empty set.
      element = TreeUtils.elementFromUse(node);
    } catch (Exception e) {
      Serializer.log("Error in finding the element from invocation: " + node);
      return Set.of();
    }
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    // Locate method receiver.
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    boolean declaredReturnTypeContainsTypeVariable =
        TypeUtils.containsTypeVariable(calledMethod.getReturnType());
    boolean hasReceiver =
        !(calledMethod.isStatic() || receiver == null || SymbolUtils.isThisIdentifier(receiver));
    AnnotatedTypeMirror.AnnotatedExecutableType methodAnnotatedType =
        typeFactory.getAnnotatedType(calledMethod);
    boolean hasPolyTaintedAnnotation =
        methodAnnotatedType != null
            && typeFactory.hasPolyTaintedAnnotation(methodAnnotatedType.getReturnType());
    boolean isGenericMethod = !calledMethod.getTypeParameters().isEmpty();
    if (hasPolyTaintedAnnotation) {
      Set<Fix> polyFixes = node.accept(new PolyMethodVisitor(typeFactory, this, context), pair);
      if (!polyFixes.isEmpty()) {
        return answer(polyFixes);
      }
    }
    if (isGenericMethod) {
      Set<Fix> fixes = node.accept(methodTypeArgumentFixVisitor, pair);
      if (!fixes.isEmpty()) {
        return answer(fixes);
      }
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    if (declaredReturnTypeContainsTypeVariable && hasReceiver) {
      Set<Fix> fixes = node.accept(new TypeArgumentFixVisitor(typeFactory, this, context), pair);
      if (!fixes.isEmpty()) {
        return answer(fixes);
      }
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    return defaultAction(node, pair);
  }

  public void reset(TreePath currentPath) {
    defaultTypeChangeVisitor.reset(currentPath);
  }

  private Set<Fix> answer(Set<Fix> fixes) {
    return fixes;
  }
}

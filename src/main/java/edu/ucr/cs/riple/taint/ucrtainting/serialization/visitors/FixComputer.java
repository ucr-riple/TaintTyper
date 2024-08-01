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
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.Config;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.TaintTyperAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CollectionHandler;
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
  protected final TaintTyperAnnotatedTypeFactory typeFactory;

  protected final Types types;
  protected final Context context;
  protected final DefaultTypeChangeVisitor defaultTypeChangeVisitor;
  protected final SpecializedFixComputer unannotatedCodeFixComputer;
  protected final SpecializedFixComputer methodTypeArgumentFixVisitor;
  protected final CollectionFixVisitor collectionFixVisitor;

  public FixComputer(
      Config config, TaintTyperAnnotatedTypeFactory factory, Types types, Context context) {
    this.typeFactory = factory;
    this.context = context;
    this.types = types;
    this.defaultTypeChangeVisitor = new DefaultTypeChangeVisitor(config, factory, this, context);
    this.unannotatedCodeFixComputer = new UnannotatedCodeFixVisitor(typeFactory, this, context);
    this.methodTypeArgumentFixVisitor = new GenericMethodFixVisitor(typeFactory, this, context);
    this.collectionFixVisitor = new CollectionFixVisitor(typeFactory, this, context);
  }

  @Override
  public Set<Fix> defaultAction(Tree node, FoundRequired pair) {
    return answer(node.accept(defaultTypeChangeVisitor, pair));
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree tree, FoundRequired pair) {
    if (tree instanceof JCTree.JCFieldAccess) {
      ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
      if (receiver != null) {
        Symbol symbol = (Symbol) TreeUtils.elementFromUse(tree);
        if (symbol.getKind().isField()
            && typeFactory.isUnannotatedField((Symbol.VarSymbol) symbol)) {
          return answer(unannotatedCodeFixComputer.visitMemberSelect(tree, pair));
        }
      }
    }
    return defaultAction(tree, pair);
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
    boolean isInAnnotatedPackage = !typeFactory.isUnannotatedMethod(calledMethod);
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
    if (CollectionHandler.isGenericToArrayMethod(calledMethod, types)) {
      return answer(node.accept(collectionFixVisitor, pair));
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
    // check if the call is to a method defined in a third party library. If the method has a type
    // var return type and has a receiver, we should annotate the receiver.
    if (!isInAnnotatedPackage && defaultTypeChangeVisitor.requireFix(pair)) {
      return answer(node.accept(unannotatedCodeFixComputer, pair));
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    return defaultAction(node, pair);
  }

  @Override
  public Set<Fix> visitNewClass(NewClassTree node, FoundRequired pair) {
    if (CollectionHandler.implementsCollectionInterface(
            (Type) pair.required.getUnderlyingType(), types)
        && CollectionHandler.implementsCollectionInterface(
            (Type) pair.found.getUnderlyingType(), types)) {
      return answer(node.accept(collectionFixVisitor, pair));
    }
    return defaultAction(node, pair);
  }

  public void reset(TreePath currentPath) {
    defaultTypeChangeVisitor.reset(currentPath);
  }

  private Set<Fix> answer(Set<Fix> fixes) {
    return fixes;
  }

  /**
   * Updates the found and required pair for the enhanced for loop error. This method computes the
   * required {@link java.util.Iterator} type from the used collection in the loop which it entries
   * match the required type. Once the iterator type is computed, it generates a new instance of
   * {@link FoundRequired} pair corresponding to the iterator type and the required type for the
   * passed collection.
   *
   * @param iterationTree The tree used in the iteration.
   * @param pair The found and required pair for the iteration variable.
   * @return The updated pair if the found type is not a subtype of the required type, null
   */
  public FoundRequired updateFoundRequiredPairEnhancedForLoopError(
      Tree iterationTree, FoundRequired pair) {
    return collectionFixVisitor.updateFoundRequiredPairEnhancedForLoopError(iterationTree, pair);
  }
}

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

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import javax.annotation.Nullable;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This handler is responsible checker custom handling for subclasses java.util.Collection. */
public class CollectionHandler extends AbstractHandler {

  /** Types instance used to detect if type is a subtype of java.util.Collection. */
  private final Types types;
  /** {@link java.util.Collection} interface name. */
  public static final String COLLECTIONS_INTERFACE = java.util.Collection.class.getName();
  /** {@link java.util.Collection#toArray()} method name. */
  private static final String TO_ARRAY_METHOD_NAME = "toArray";

  public CollectionHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    super(typeFactory);
    this.types = Types.instance(context);
  }

  /**
   * Checks if the receiver is subtype of Collection<@Untainted T> and the method is toArray(T[]),
   * then it will make the component type of the array untainted.
   *
   * @param tree The method invocation tree.
   * @param type The annotated type of the method invocation.
   */
  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    // check if the method is T[] Collection.toArray(T[]) is called.
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (!(type instanceof AnnotatedTypeMirror.AnnotatedArrayType)) {
      return;
    }
    if (!isGenericToArrayMethod(calledMethod, types)) {
      return;
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (receiver == null || SymbolUtils.isThisIdentifier(receiver)) {
      return;
    }
    if (!(((JCTree) receiver).type instanceof Type.ClassType)) {
      throw new RuntimeException("CollectionHandler: receiver is not a class type");
    }
    AnnotatedTypeMirror receiverType = typeFactory.getReceiverType(tree);
    if (receiverType == null) {
      return;
    }
    Type.ClassType collectionType =
        retrieveCollectionTypeMirrorFromType(receiverType.getUnderlyingType());
    if (collectionType == null) {
      return;
    }
    // check if used raw type
    if (collectionType.typarams_field.isEmpty()) {
      return;
    }
    // check if the type argument is annotated with @Untainted
    if (TypeUtils.hasUntaintedAnnotation(collectionType.typarams_field.get(0))) {
      // make the component type of the array untainted
      typeFactory.makeUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
    // check if the type argument is annotated with @PolyTainted
    if (TypeUtils.hasPolyTaintedAnnotation(collectionType.typarams_field.get(0))) {
      // make the component type of the array poly tainted
      typeFactory.makePolyTainted(
          ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    super.visitNewClass(tree, type);
    // if no arguments are passed, then return.
    if (tree.getArguments().isEmpty()) {
      return;
    }
    // check both the new class and the passed argument are subtypes of Collection
    if (!implementsCollectionInterface(TypeUtils.getType(tree), types)) {
      return;
    }
    ExpressionTree arg = tree.getArguments().get(0);
    if (!implementsCollectionInterface(TypeUtils.getType(arg), types)) {
      return;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType argAnnotatedType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) typeFactory.getAnnotatedType(arg);
    AnnotatedTypeMirror.AnnotatedDeclaredType newClassType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
    if (argAnnotatedType.getTypeArguments().isEmpty()) {
      return;
    }
    Type.ClassType argumentCollectionType =
        retrieveCollectionTypeMirrorFromType(argAnnotatedType.getUnderlyingType());
    if (argumentCollectionType == null || argumentCollectionType.isRaw()) {
      return;
    }
    if (TypeUtils.hasUntaintedAnnotation(argumentCollectionType.typarams_field.get(0))) {
      typeFactory.makeUntainted(newClassType.getTypeArguments().get(0));
    }
    if (TypeUtils.hasPolyTaintedAnnotation(argumentCollectionType.typarams_field.get(0))) {
      typeFactory.makePolyTainted(newClassType.getTypeArguments().get(0));
    }
  }

  /**
   * Check if the method is {@link java.util.Collection#toArray(Object[])} .
   *
   * @param symbol The method symbol to check.
   * @param types The types instance.
   * @return True if the method is {@link java.util.Collection#toArray(Object[])}.
   */
  public static boolean isGenericToArrayMethod(Symbol.MethodSymbol symbol, Types types) {
    // Check method name
    if (!symbol.getSimpleName().toString().equals(TO_ARRAY_METHOD_NAME)) {
      return false;
    }
    // Check if the method is generic
    if (symbol.getTypeParameters().size() != 1) {
      return false;
    }
    // Check if the method has a single parameter.
    if (symbol.getParameters().size() != 1) {
      return false;
    }
    // check param is of type T[]
    Symbol.VarSymbol param = symbol.getParameters().get(0);
    if (!(param.asType() instanceof Type.ArrayType
        && ((ArrayType) param.asType()).getComponentType() instanceof Type.TypeVar)) {
      return false;
    }
    // Check if the return type is T[]
    if (!(symbol.getReturnType() instanceof Type.ArrayType
        && ((ArrayType) symbol.getReturnType()).getComponentType() instanceof Type.TypeVar)) {
      return false;
    }
    // Check if class is subclass of Collection interface
    return implementsCollectionInterface(symbol.enclClass().type, types);
  }

  /**
   * Check if the type is subtype of the {@link java.util.Collection} interface.
   *
   * @param type The type to check.
   * @return True if the type is subtype of the {@link java.util.Collection} interface.
   */
  public static boolean implementsCollectionInterface(Type type, Types types) {
    if (type == null || type.tsym == null) {
      return false;
    }
    if (type.isInterface() && type.tsym.toString().equals(COLLECTIONS_INTERFACE)) {
      return true;
    }
    boolean implementsCollection =
        types.interfaces(type).stream()
            .anyMatch(intFace -> implementsCollectionInterface(intFace, types));
    if (implementsCollection) {
      return true;
    }
    return implementsCollectionInterface(types.supertype(type), types);
  }

  /**
   * Retrieve the {@link java.util.Collection} type mirror from the given type. If the type is not a
   * subtype of {@link java.util.Collection}, then null is returned. (e.g. given {@code
   * Foo<@Untainted String>} where {@code Foo<T> implements Collection<T>} will return {@code
   * Collection<@Untainted String>}
   *
   * @param type The type to retrieve the {@link java.util.Collection} type from.
   * @return The {@link java.util.Collection} type from the given type.
   */
  @Nullable
  public static Type.ClassType retrieveCollectionTypeMirrorFromType(TypeMirror type) {
    if (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      if (classType.tsym instanceof Symbol.ClassSymbol
          && ((Symbol.ClassSymbol) classType.tsym)
              .fullname
              .toString()
              .equals(COLLECTIONS_INTERFACE)) {
        // found collection interface
        return classType;
      }
    }
    while (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      if (classType.interfaces_field != null) {
        // check implemented interfaces
        for (Type iFace : classType.interfaces_field) {
          Type.ClassType collectionType = retrieveCollectionTypeMirrorFromType(iFace);
          if (collectionType != null) {
            // found collection interface
            return collectionType;
          }
        }
      }
      // check super type
      type = ((Type.ClassType) type).supertype_field;
    }
    return null;
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Objects;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class UnannotatedCodeHandler extends AbstractHandler {

  private static final ImmutableSet<MethodRef> approvedThirdPartyMethod =
      ImmutableSet.of(
          new MethodRef("java.lang.Class", "cast(java.lang.Object)"),
          new MethodRef("java.lang.ClassLoader", "loadClass(java.lang.String)"));

  public UnannotatedCodeHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    Element selected = TreeUtils.elementFromUse(tree);
    // if already we don't have to do all the computations below.
    if (typeFactory.isPolyOrUntainted(type)) {
      return;
    }
    if (!selected.getKind().isField()) {
      // if not field and is an invocation, we should handle it in visitMethodInvocation call.
      return;
    }
    if (tree instanceof JCTree.JCFieldAccess) {
      ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
      if (receiver == null) {
        return;
      }
      Symbol symbol = (Symbol) TreeUtils.elementFromUse(receiver);
      String packageName = symbol.type.tsym.packge().toString();
      if (packageName.equals("unnamed package")) {
        packageName = "";
      }
      if (typeFactory.isUnAnnotatedPackageName(packageName)
          && (ElementUtils.isStatic(selected) || (!typeFactory.mayBeTainted(receiver)))) {
        typeFactory.makeUntainted(type);
      }
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    if (!checkHeuristicApplicability(tree, typeFactory)) {
      return;
    }
    typeFactory.makeUntainted(type);
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      typeFactory.makeUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
  }

  /**
   * Determines if the heuristic is applicable to the passed invocation tree.
   *
   * @param tree the invocation tree.
   * @param factory the type factory of the checker.
   * @return true if the heuristic is applicable, false otherwise.
   */
  public static boolean checkHeuristicApplicability(
      MethodInvocationTree tree, UCRTaintingAnnotatedTypeFactory factory) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    // Check if the method is approved third party method.
    MethodRef methodRef =
        new MethodRef(
            Serializer.serializeSymbol(calledMethod.enclClass()),
            Serializer.serializeMethodSignature(calledMethod));
    if (approvedThirdPartyMethod.contains(methodRef)) {
      return true;
    }

    // If already untainted, it should be acknowledged
    if (Utility.hasUntaintedAnnotation(calledMethod.getReturnType())) {
      return true;
    }
    if (!factory.isThirdPartyMethod(calledMethod)) {
      return false;
    }
    // Check receiver, if receiver is tainted, we should not make it untainted.
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    boolean hasValidReceiver =
        receiver == null
            || calledMethod.isStatic()
            || factory.isPolyOrUntainted(receiver)
            || Utility.isSuperIdentifier(receiver);
    if (!hasValidReceiver) {
      return false;
    }
    // Check passed arguments, if any of them is tainted, we should not make it untainted.
    boolean noTaintedParams =
        tree.getArguments().stream()
            .allMatch(expressionTree -> polyOrUntaintedParameter(expressionTree, factory));
    if (noTaintedParams) {
      return hasInvariantReturnType(tree);
    }
    return false;
  }

  /**
   * Determines if the invocation has an invariant return type. This is used to determine if the
   * mismatch in the return type can be fixed by adding an annotation either on the receiver or the
   * passed arguments.
   *
   * @param tree the invocation tree.
   * @return true if the return type is invariant, false otherwise.
   */
  public static boolean hasInvariantReturnType(MethodInvocationTree tree) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    Type returnType = calledMethod.getReturnType();
    if (calledMethod.isStatic() && calledMethod.params.isEmpty()) {
      return true;
    }
    // check method type arguments
    if (calledMethod.type.getTypeArguments().stream()
        .anyMatch(type -> type.tsym.name.equals(returnType.tsym.name))) {
      return false;
    }
    if (receiver == null) {
      return true;
    }
    if (receiver instanceof JCTree.JCLiteral) {
      // e.g. "bar".equals()
      return true;
    }
    Element receiverElement;
    try {
      receiverElement = TreeUtils.elementFromUse(receiver);
    } catch (Exception e) {
      return true;
    }
    if (Utility.elementHasRawType(receiverElement)) {
      return true;
    }
    return Utility.getType(receiverElement).tsym.type.getTypeArguments().stream()
        .noneMatch(type -> Utility.containsTypeArgument(returnType, (Type.TypeVar) type));
  }

  /**
   * Determines if the passed argument is either poly or untainted.
   *
   * @param argument the argument to check.
   * @return true if the argument is either poly or untainted, false otherwise.
   */
  public static boolean polyOrUntaintedParameter(
      ExpressionTree argument, UCRTaintingAnnotatedTypeFactory typeFactory) {
    AnnotatedTypeMirror type = typeFactory.getAnnotatedType(argument);
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      // For third party heuristics, we only care about the component type of the array.
      type = ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
    }
    if (typeFactory.isPolyOrUntainted(type)) {
      return true;
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      return typeFactory.isPolyOrUntainted(
          ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
    return false;
  }

  private static class MethodRef {
    public final String method;
    public final String className;

    public MethodRef(String className, String method) {
      this.method = method;
      this.className = className;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MethodRef)) {
        return false;
      }
      MethodRef methodRef = (MethodRef) o;
      return Objects.equals(method, methodRef.method)
          && Objects.equals(className, methodRef.className);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, className);
    }
  }
}

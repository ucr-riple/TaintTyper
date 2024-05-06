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
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import java.util.Objects;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class UnannotatedCodeHandler extends AbstractHandler {

  /**
   * Set of third party {@link MethodRef} that are identified as polymorphic. These methods fail the
   * heuristic applicability check since they contain a type variable in their return type, but they
   * are known to be polymorphic. This is mostly to complicated structure of {@code Class<T>} type.
   */
  private static final ImmutableSet<MethodRef> identifiedPolyMorphicThirdPartyMethod =
      ImmutableSet.of(new MethodRef("java.lang.Class", "cast(java.lang.Object)"));

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
    Symbol.VarSymbol field = (Symbol.VarSymbol) selected;
    if (!typeFactory.isUnannotatedField(field)) {
      return;
    }
    if (ElementUtils.isStatic(field)) {
      typeFactory.makeUntainted(type);
      return;
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (receiver != null && !typeFactory.mayBeTainted(receiver)) {
      typeFactory.makeUntainted(type);
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    if (!isSafeTransitionToUnAnnotatedCode(tree, typeFactory)) {
      return;
    }
    typeFactory.makeUntainted(type);
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      typeFactory.makeUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
  }

  /**
   * Determines if the invocation is a safe transition to unannotated code. A safe transition is a
   * method invocation from annotated to unannotated with {@code @Untainted} receiver and arguments.
   * Please note that the method itself must be identified as unannotated polymorphic method. If the
   * return type can be controlled using a type argument provided in annotated code, we refrain from
   * applying the heuristic. (e.g. {@code list.get(0)} even thought the receiver can be untainted,
   * the return type is controlled by the list type argument, hence, we should not make it
   * untainted.)
   *
   * @param tree the invocation tree.
   * @param factory the type factory of the checker.
   * @return true if the heuristic is applicable, false otherwise.
   */
  public static boolean isSafeTransitionToUnAnnotatedCode(
      MethodInvocationTree tree, UCRTaintingAnnotatedTypeFactory factory) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    // If already untainted, it should be acknowledged
    if (TypeUtils.hasUntaintedAnnotation(calledMethod.getReturnType())) {
      return true;
    }
    if (!factory.isUnannotatedMethod(calledMethod)) {
      return false;
    }
    // Check receiver, if receiver is tainted, we should not make it untainted.
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (!checkMethodIsUnAnnotatedPolymorphic(receiver, calledMethod)) {
      return false;
    }
    boolean hasValidReceiver =
        receiver == null
            || receiver instanceof JCTree.JCLiteral
            || calledMethod.isStatic()
            || factory.isPolyOrUntainted(receiver)
            || SymbolUtils.isSuperIdentifier(receiver);
    if (!hasValidReceiver) {
      return false;
    }
    // Check passed arguments, if any of them is tainted, we should not make it untainted.
    return tree.getArguments().stream()
        .allMatch(expressionTree -> polyOrUntaintedParameter(expressionTree, factory));
  }

  /**
   * Determines if a method is unannotated polymorphic. A method is unannotated polymorphic if the
   * method is from unannotated code and the return type cannot be controlled by a type argument
   * provided in annotated code.
   *
   * @param calledMethod the invocation tree. * @param receiver the receiver of the method
   *     invocation.
   * @return true if the method is unannotated polymorphic, false otherwise.
   */
  private static boolean checkMethodIsUnAnnotatedPolymorphic(
      ExpressionTree receiver, Symbol.MethodSymbol calledMethod) {
    if (receiver == null) {
      return true;
    }
    // Check if the method is approved third party method.
    MethodRef methodRef =
        new MethodRef(
            Serializer.serializeSymbol(calledMethod.enclClass()),
            Serializer.serializeMethodSignature(calledMethod));
    if (identifiedPolyMorphicThirdPartyMethod.contains(methodRef)) {
      return true;
    }
    Type returnType = calledMethod.getReturnType();
    if (calledMethod.isStatic() && calledMethod.params.isEmpty()) {
      return true;
    }
    // if method is generic, check if the return type is one of the methods type variables.
    if (calledMethod.type.getTypeArguments().stream()
        .anyMatch(type -> type.tsym.name.equals(returnType.tsym.name))) {
      // The method is generic and the return type is one of the type variables, the return type
      // should be determined by the passed arguments.
      return false;
    }
    if (TypeUtils.hasRawType(receiver)) {
      // if raw type, we can just optimistically assume that the method is polymorphic.
      return true;
    }
    // Check if return type is one of the type variables of the receiver.
    return TypeUtils.getType(receiver).tsym.type.getTypeArguments().stream()
        .noneMatch(type -> TypeUtils.containsTypeVariable(returnType, (Type.TypeVar) type));
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

  /** Represents a method reference with class name and method name. */
  private static class MethodRef {
    /** The method name. */
    public final String method;
    /** The class name. */
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

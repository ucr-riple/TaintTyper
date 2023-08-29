package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class ThirdPartyHandler extends AbstractHandler {

  public ThirdPartyHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    // If already untainted, it should be acknowledged
    if (Utility.hasUntaintedAnnotation(calledMethod.getReturnType())) {
      typeFactory.makeUntainted(type);
      return;
    }
    if (!typeFactory.isInThirdPartyCode(calledMethod)) {
      return;
    }
    if (typeFactory.isSource(calledMethod)) {
      return;
    }
    // Check receiver, if receiver is tainted, we should not make it untainted.
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    boolean hasValidReceiver =
        receiver == null || calledMethod.isStatic() || typeFactory.isPolyOrUntainted(receiver);
    if (!hasValidReceiver) {
      return;
    }
    // Check passed arguments, if any of them is tainted, we should not make it untainted.
    boolean noTaintedParams = tree.getArguments().stream().allMatch(this::polyOrUntaintedParameter);
    if (noTaintedParams) {
      if (shouldApplyHeuristic(receiver, calledMethod)) {
        typeFactory.makeDeepUntainted(type);
      }
    }
  }

  private boolean shouldApplyHeuristic(ExpressionTree receiver, Symbol.MethodSymbol calledMethod) {
    Type returnType = calledMethod.getReturnType();
    // check method type arguments
    if (calledMethod.type.getTypeArguments().stream()
        .anyMatch(type -> Utility.containsTypeArgument(returnType, (Type.TypeVar) type))) {
      return false;
    }
    if (receiver == null) {
      return true;
    }
    Element receiverElement = TreeUtils.elementFromUse(receiver);
    return Utility.getType(receiverElement).tsym.type.getTypeArguments().stream()
        .noneMatch(type -> Utility.containsTypeArgument(returnType, (Type.TypeVar) type));
  }

  private boolean polyOrUntaintedParameter(ExpressionTree argument) {
    AnnotatedTypeMirror type = typeFactory.getAnnotatedType(argument);
    if (typeFactory.isPolyOrUntainted(type)) {
      return true;
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      return typeFactory.isPolyOrUntainted(
          ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
    return false;
  }
}

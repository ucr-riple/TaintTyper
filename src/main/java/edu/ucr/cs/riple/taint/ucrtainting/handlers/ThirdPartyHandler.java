package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
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
    if (!typeFactory.isUnannotatedThirdParty(tree)) {
      return;
    }
    // Check receiver, if receiver is tainted, we should not make it untainted.
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (receiver == null) {
      return;
    }
    boolean hasValidReceiver = calledMethod.isStatic() || typeFactory.isPolyOrUntainted(receiver);
    if (!hasValidReceiver) {
      return;
    }
    // Check passed arguments, if any of them is tainted, we should not make it untainted.
    boolean hasUntaintedAnnotations =
        tree.getArguments().stream().allMatch(typeFactory::isPolyOrUntainted);
    if (hasUntaintedAnnotations) {
      if (calledMethod.isStatic() || !(calledMethod.getReturnType() instanceof Type.TypeVar)) {
        typeFactory.makeUntainted(type);
      }
    }
  }
}

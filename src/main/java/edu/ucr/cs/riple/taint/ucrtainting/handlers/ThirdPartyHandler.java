package edu.ucr.cs.riple.taint.ucrtainting.handlers;

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
    // If the receiver is untainted and all arguments are untainted, the result should be untainted.
    if (typeFactory.isUnannotatedThirdParty(tree)) {
      if (!typeFactory.hasTaintedArgument(tree) && !typeFactory.hasTaintedReceiver(tree)) {
        Type returnType = calledMethod.getReturnType();
        if (calledMethod.isStatic() || !(returnType instanceof Type.TypeVar)) {
          typeFactory.makeUntainted(type);
        }
      }
    }
  }
}

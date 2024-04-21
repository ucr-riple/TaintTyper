package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This handler is responsible for making annotation members untainted. */
public class AnnotationMemberHandler extends AbstractHandler {

  public AnnotationMemberHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (receiver == null) {
      return;
    }
    Element receiverElem = TreeUtils.elementFromTree(receiver);
    if (!(receiverElem instanceof Symbol.VarSymbol)) {
      return;
    }
    Symbol.VarSymbol receiverVar = (Symbol.VarSymbol) receiverElem;
    if (receiverVar.type.tsym.isAnnotationType()) {
      typeFactory.makeUntainted(type);
    }
  }
}

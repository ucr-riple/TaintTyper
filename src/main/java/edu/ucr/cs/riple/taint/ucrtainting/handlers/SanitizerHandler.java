package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This handler is responsible for making the invocation of stub methods untainted if the invoked
 * method is annotated as untainted. Note: not sure why this is necessary, as the annotations in the
 * stubs should be enough to determine the taint of the method.
 */
public class SanitizerHandler extends AbstractHandler {

  public SanitizerHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    Element element = TreeUtils.elementFromUse(tree);
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
    if (!typeFactory.isFromStubFile(methodSymbol)) {
      return;
    }
    AnnotatedTypeMirror annotatedTypeMirror =
        typeFactory.stubTypes.getAnnotatedTypeMirror(methodSymbol);
    if (!(annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedExecutableType)) {
      return;
    }
    AnnotatedTypeMirror.AnnotatedExecutableType executableType =
        (AnnotatedTypeMirror.AnnotatedExecutableType) annotatedTypeMirror;
    if (typeFactory.hasUntaintedAnnotation(executableType.getReturnType())) {
      typeFactory.makeUntainted(type);
    }
    super.visitMethodInvocation(tree, type);
  }
}

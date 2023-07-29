package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.MethodInvocationTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public abstract class AbstractHandler implements Handler {

  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  public AbstractHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  @Override
  public void visitField(Element element, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    // no-op
  }
}

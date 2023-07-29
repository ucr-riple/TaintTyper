package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public interface Handler {

  void visitField(Element element, AnnotatedTypeMirror type);

  void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type);
}

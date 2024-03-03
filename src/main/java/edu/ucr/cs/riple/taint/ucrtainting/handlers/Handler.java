package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public interface Handler {

  void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type);

  void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type);

  void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type);

  void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type);

  LambdaHandler getLambdaHandler();
}

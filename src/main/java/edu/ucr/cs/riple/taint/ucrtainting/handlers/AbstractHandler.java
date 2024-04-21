package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public abstract class AbstractHandler implements Handler {

  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  public AbstractHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
    // no-op
  }

  @Override
  public void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    // no-op
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class CompositHandler implements Handler {

  /** Set of handlers to be used to add annotations from default for type. */
  private final ImmutableSet<Handler> handlers;

  public CompositHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    ImmutableSet.Builder<Handler> handlerBuilder = new ImmutableSet.Builder<>();
    handlerBuilder.add(new StaticFinalFieldHandler(typeFactory));
    handlerBuilder.add(new EnumHandler(typeFactory));
    if (typeFactory.libraryCheckIsEnabled()) {
      handlerBuilder.add(new ThirdPartyHandler(typeFactory));
    }
    handlerBuilder.add(new CollectionHandler(typeFactory, context));
    handlerBuilder.add(new AnnotationHandler(typeFactory));
    this.handlers = handlerBuilder.build();
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.addAnnotationsFromDefaultForType(element, type));
  }

  @Override
  public void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitVariable(variableTree, type));
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitMethodInvocation(tree, type));
  }

  @Override
  public void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
    this.handlers.forEach(handler -> handler.visitMemberSelect(tree, type));
  }
}

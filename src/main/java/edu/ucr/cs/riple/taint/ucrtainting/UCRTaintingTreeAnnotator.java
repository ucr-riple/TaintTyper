package edu.ucr.cs.riple.taint.ucrtainting;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.List;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

public class UCRTaintingTreeAnnotator extends TreeAnnotator {
  private final ImmutableSet<Handler> handlers;
  private final UCRTaintingAnnotatedTypeFactory typeFactory;

  /**
   * UCRTaintingTreeAnnotator
   *
   * @param typeFactory the type factory
   */
  protected UCRTaintingTreeAnnotator(
      UCRTaintingAnnotatedTypeFactory typeFactory, ImmutableSet<Handler> handlers) {
    super(typeFactory);
    this.typeFactory = typeFactory;
    this.handlers = handlers;
  }

  /**
   * Visits all method invocations and updates {@link AnnotatedTypeMirror} according to the argument
   * and receiver annotations. If any of the arguments or the receiver is {@link RTainted}, the
   * {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated return type of the method invocation
   */
  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      handlers.forEach(handler -> handler.visitMethodInvocation(node, annotatedTypeMirror));
    }
    return super.visitMethodInvocation(node, annotatedTypeMirror);
  }

  @Override
  public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      handlers.forEach(handler -> handler.visitVariable(node, annotatedTypeMirror));
    }
    return super.visitVariable(node, annotatedTypeMirror);
  }

  @Override
  public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      typeFactory.makeUntainted(annotatedTypeMirror);
    }
    return super.visitLiteral(node, annotatedTypeMirror);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      if (Utility.isEnumConstant(node)) {
        typeFactory.makeUntainted(annotatedTypeMirror);
      }
      Element element = TreeUtils.elementFromUse(node);
      // check if is final and static
      if (Utility.isStaticAndFinal(element) && element.getKind().isField()) {
        if (node instanceof JCTree.JCFieldAccess) {
          typeFactory.makeUntainted(annotatedTypeMirror);
        }
      }
    }
    return super.visitMemberSelect(node, annotatedTypeMirror);
  }

  /**
   * Visits all new class creations and updates {@link AnnotatedTypeMirror} according to the
   * argument and receiver annotations. If any of the arguments or the receiver is {@link RTainted},
   * the {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated type of the new class
   */
  @Override
  public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      // if the code is part of provided annotated packages or is present
      // in the stub files, then we don't need any custom handling for it.
      if (typeFactory.isInThirdPartyCode(node) && !typeFactory.isPresentInStub(node)) {
        if (!(typeFactory.hasTaintedArgument(node) || typeFactory.hasTaintedReceiver(node))) {
          typeFactory.makeUntainted(annotatedTypeMirror);
        }
      } else {
        if (!typeFactory.hasTaintedArgument(node)) {
          typeFactory.makeUntainted(annotatedTypeMirror);
        }
      }
    }
    return super.visitNewClass(node, annotatedTypeMirror);
  }

  @Override
  public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror mirror) {
    List<? extends ExpressionTree> initializers = node.getInitializers();
    boolean allUntainted = true;
    if (initializers != null) {
      for (ExpressionTree initializer : initializers) {
        if (typeFactory.mayBeTainted(initializer)) {
          allUntainted = false;
          break;
        }
      }
    }
    if (allUntainted) {
      typeFactory.makeUntainted(mirror);
    }
    return super.visitNewArray(node, mirror);
  }
}

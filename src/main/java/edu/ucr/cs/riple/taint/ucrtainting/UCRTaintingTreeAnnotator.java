package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

public class UCRTaintingTreeAnnotator extends TreeAnnotator {
  private final Handler handler;
  private final UCRTaintingAnnotatedTypeFactory typeFactory;
  private final Context context;
  private final Map<Symbol, Tree> symbolToDeclarationMap;

  /**
   * UCRTaintingTreeAnnotator
   *
   * @param typeFactory the type factory
   * @param context The javac context.
   */
  protected UCRTaintingTreeAnnotator(
      UCRTaintingAnnotatedTypeFactory typeFactory, Handler handler, Context context) {
    super(typeFactory);
    this.typeFactory = typeFactory;
    this.handler = handler;
    this.context = context;
    this.symbolToDeclarationMap = new HashMap<>();
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
      handler.visitMethodInvocation(node, annotatedTypeMirror);
    }
    return super.visitMethodInvocation(node, annotatedTypeMirror);
  }

  @Override
  public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    if (typeFactory.customCheckIsEnabled()) {
      handler.visitVariable(node, annotatedTypeMirror);
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
      Symbol symbol = (Symbol) TreeUtils.elementFromUse(node);
      Tree selected =
          symbolToDeclarationMap.containsKey(symbol)
              ? symbolToDeclarationMap.get(symbol)
              : Utility.locateDeclaration(symbol, context);
      symbolToDeclarationMap.putIfAbsent(symbol, selected);
      if (selected != null && !typeFactory.mayBeTainted(selected)) {
        typeFactory.makeUntainted(annotatedTypeMirror);
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

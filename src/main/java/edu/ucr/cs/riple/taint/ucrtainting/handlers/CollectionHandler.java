package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class CollectionHandler extends AbstractHandler {

  public CollectionHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (!typeFactory.isSideEffectFree(symbol)) {
      return;
    }
    System.out.println("CollectionHandler: " + symbol);
  }

  private void overridesCollectionInterface() {
    // todo: merge from branch
  }

  private boolean isToArrayWithTypeArgMethod(Symbol.MethodSymbol symbol) {
    // todo: merge from branch
    return symbol.name.toString().equals("toArray") && symbol.params().size() == 1;
  }
}

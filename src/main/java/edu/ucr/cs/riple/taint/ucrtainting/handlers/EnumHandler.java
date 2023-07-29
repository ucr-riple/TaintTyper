package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class EnumHandler extends AbstractHandler {

  public EnumHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitVariable(VariableTree tree, AnnotatedTypeMirror type) {
    if (Utility.isEnumConstant(tree)) {
      ExpressionTree initializer = tree.getInitializer();
      if (!typeFactory.hasTaintedArgument(initializer)) {
        typeFactory.makeUntainted(typeFactory.getAnnotatedType(initializer));
      }
      typeFactory.makeUntainted(type);
    }
  }
}

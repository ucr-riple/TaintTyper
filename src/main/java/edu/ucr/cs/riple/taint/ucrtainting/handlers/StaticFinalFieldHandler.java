package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class StaticFinalFieldHandler extends AbstractHandler {

  private final Set<Element> staticFinalFields;

  public StaticFinalFieldHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
    this.staticFinalFields = new HashSet<>();
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    if (staticFinalFields.contains(element)) {
      typeFactory.makeUntainted(type);
      return;
    }
    if (Utility.isStaticAndFinalField(element)) {
      if (typeFactory.isInThirdPartyCode(element)) {
        typeFactory.makeUntainted(type);
      } else {
        Tree decl = typeFactory.declarationFromElement(element);
        if (decl instanceof VariableTree) {
          ExpressionTree initializer = ((VariableTree) decl).getInitializer();
          if (Utility.isLiteralOrPrimitive(initializer)) {
            staticFinalFields.add(element);
            typeFactory.makeUntainted(type);
          }
        }
      }
    }
  }

  @Override
  public void visitVariable(VariableTree tree, AnnotatedTypeMirror type) {
    Element element = TreeUtils.elementFromDeclaration(tree);
    if (staticFinalFields.contains(element)) {
      typeFactory.makeUntainted(type);
      return;
    }
    // check if is final and static
    if (Utility.isStaticAndFinalField(element)) {
      ExpressionTree initializer = tree.getInitializer();
      // check if initializer is a literal or a primitive
      if (Utility.isLiteralOrPrimitive(initializer)) {
        staticFinalFields.add(element);
        typeFactory.makeUntainted(type);
      }
    }
  }
}

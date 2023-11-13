package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class StaticFinalFieldHandler extends AbstractHandler {

  private final Set<Element> staticFinalFields;
  private final Map<ExpressionTree, InitializerState> visitedInitializers;

  enum InitializerState {
    UNKNOWN,
    UNTAINTED,
    TAINTED
  }

  public StaticFinalFieldHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
    this.staticFinalFields = new HashSet<>();
    this.visitedInitializers = new HashMap<>();
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
          if (isUntaintedInitializer(initializer)) {
            staticFinalFields.add(element);
            typeFactory.makeUntainted(type);
            if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
              typeFactory.makeDeepUntainted(
                  ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
            }
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
      if (isUntaintedInitializer(initializer)) {
        staticFinalFields.add(element);
        typeFactory.makeUntainted(type);
      }
    }
  }

  private boolean isUntaintedInitializer(ExpressionTree initializer) {
    if (visitedInitializers.containsKey(initializer)
        && visitedInitializers.get(initializer) == InitializerState.UNKNOWN) {
      // to prevent loop.
      return false;
    }
    if (visitedInitializers.containsKey(initializer)) {
      return visitedInitializers.get(initializer) == InitializerState.UNTAINTED;
    }
    visitedInitializers.put(initializer, InitializerState.UNKNOWN);
    boolean isUntaintedInitializer;
    try {
      isUntaintedInitializer = typeFactory.hasUntaintedAnnotation(initializer);
    } catch (Exception e) {
      isUntaintedInitializer = false;
    }
    visitedInitializers.put(
        initializer,
        isUntaintedInitializer ? InitializerState.UNTAINTED : InitializerState.TAINTED);
    return isUntaintedInitializer;
  }
}

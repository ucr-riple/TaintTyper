package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Handler for lambda expressions. Lambda expressions parameters are considered untainted if the
 * overridden method is from unannotated code.
 */
public class LambdaHandler extends AbstractHandler {

  /**
   * Set of lambda parameters that are visited previously by this handler. Parameters added to this
   * handler will be considered as untainted if the overridden method is from unannotated code.
   */
  private final Set<Symbol.VarSymbol> lambdaParameters;

  private final Types types;

  public LambdaHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    super(typeFactory);
    this.lambdaParameters = new HashSet<>();
    this.types = Types.instance(context);
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    if (element instanceof Symbol.VarSymbol && lambdaParameters.contains(element)) {
      typeFactory.makeUntainted(type);
    }
  }

  @Override
  public void visitLambdaExpression(
      LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
    typeFactory.makeUntainted(annotatedTypeMirror);
    Symbol.MethodSymbol overriddenMethod = SymbolUtils.getFunctionalInterfaceMethod(node, types);
    if (overriddenMethod != null && typeFactory.isUnannotatedMethod(overriddenMethod)) {
      node.getParameters()
          .forEach(
              variableTree ->
                  this.lambdaParameters.add(
                      (Symbol.VarSymbol) TreeUtils.elementFromDeclaration(variableTree)));
    }
    super.visitLambdaExpression(node, annotatedTypeMirror);
  }
}

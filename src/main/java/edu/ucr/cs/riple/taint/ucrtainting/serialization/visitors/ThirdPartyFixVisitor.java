package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Visitor for handling calls to third party libraries. This visitor gets to the required type by
 * making receiver and all arguments be untainted.
 */
public class ThirdPartyFixVisitor extends SpecializedFixComputer {

  public ThirdPartyFixVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer) {
    super(context, factory, fixComputer);
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    // check if the call is to a method defined in a third party library.

    // Check if the method is source defined in stubs.
    if (typeFactory.isSource(calledMethod)) {
      // We cannot do any fix here
      return Set.of();
    }
    Set<Fix> fixes = new HashSet<>();
    // Check if any of the argument is a lambda expression.
    for (ExpressionTree argument : node.getArguments()) {
      if (argument instanceof LambdaExpressionTree) {
        // We cannot do any fix here
        return Set.of();
      }
    }
    // Add a fix for each passed argument.
    for (int i = 0; i < node.getArguments().size(); i++) {
      ExpressionTree argument = node.getArguments().get(i);
      Symbol.VarSymbol formalParameter = calledMethod.params().get(i);
      AnnotatedTypeMirror formalParameterType =
          typeFactory.getAnnotatedType(formalParameter).deepCopy(true);
      makeUntainted(formalParameterType, typeFactory);
      AnnotatedTypeMirror actualParameterType = typeFactory.getAnnotatedType(argument);
      fixes.addAll(
          argument.accept(fixComputer, FoundRequired.of(actualParameterType, formalParameterType)));
    }
    // Add the fix for the receiver if not static.
    if (calledMethod.isStatic()) {
      // No receiver for static method calls.
      return fixes;
    }
    // Build the fix for the receiver.
    fixes.addAll(
        ((MemberSelectTree) node.getMethodSelect()).getExpression().accept(fixComputer, pair));
    return fixes;
  }

  /**
   * Makes the given type untainted. If the type is an array, the component type is made untainted.
   * Do not use or change {@link UCRTaintingAnnotatedTypeFactory#makeUntainted} method.
   *
   * @param type The type to make untainted.
   * @param factory The type factory.
   */
  private void makeUntainted(AnnotatedTypeMirror type, UCRTaintingAnnotatedTypeFactory factory) {
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      ((AnnotatedTypeMirror.AnnotatedArrayType) type)
          .getComponentType()
          .replaceAnnotation(factory.rUntainted);
    } else {
      type.replaceAnnotation(factory.rUntainted);
    }
  }
}

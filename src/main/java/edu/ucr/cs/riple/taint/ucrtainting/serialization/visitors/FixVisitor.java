package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/**
 * General Fix visitor.This visitor determines the approach for resolving the error upon visiting
 * specific nodes that may impact the algorithm selection.
 */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  private final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;
  /**
   * The pair of found annotated type and the required annotated type. This visitor uses this
   * information to generated annotations that adapts the found type to the required type.
   */
  protected final FoundRequired pair;

  public FixVisitor(Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    this.context = context;
    this.typeFactory = factory;
    this.pair = pair;
  }

  @Override
  public Set<Fix> defaultAction(Tree node, Void unused) {
    return node.accept(new BasicVisitor(context, typeFactory, pair), null);
  }

  /**
   * Visitor for method invocations. In method invocations, we might choose different approaches:
   *
   * <ol>
   *   <li>If in stub files, exit
   *   <li>If method has type args, and by changing the parameter types of parameters, we can
   *       achieve required type, we annotate the parameters.
   *   <li>If return type of method has type arguments and the call has a valid receiver, we
   *       annotate the receiver.
   *   <li>If method is in third party library, we annotate the receiver and parameters.
   *   <li>Annotate the called method directly
   * </ol>
   *
   * @param node The given tree.
   * @return Void null.
   */
  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Void unused) {
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    if (typeFactory.isPresentInStub(node.getMethodSelect())) {
      return Set.of();
    }
    // Locate method receiver.
    ExpressionTree receiver = null;
    if (node.getMethodSelect() instanceof MemberSelectTree) {
      receiver = ((MemberSelectTree) node.getMethodSelect()).getExpression();
    }
    boolean isInAnnotatedPackage = Utility.isInAnnotatedPackage(calledMethod, typeFactory);
    boolean isTypeVar = Utility.containsTypeArgument(calledMethod.getReturnType());
    boolean hasReceiver =
        !(calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver));
    boolean methodHasTypeArgs = !calledMethod.getTypeParameters().isEmpty();
    if (methodHasTypeArgs) {
      return node.accept(new MethodTypeArgumentFixVisitor(context, typeFactory, pair), unused);
    }
    // check if the call is to a method defined in a third party library. If the method has a type
    // var return type and has a receiver, we should annotate the receiver.
    if (!isInAnnotatedPackage && !(isTypeVar && hasReceiver)) {
      return node.accept(new ThirdPartyFixVisitor(context, typeFactory), unused);
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    if (isTypeVar && hasReceiver) {
      return node.accept(new ReceiverTypeParameterFixVisitor(context, typeFactory), unused);
    } else {
      return defaultAction(node, unused);
    }
  }
}

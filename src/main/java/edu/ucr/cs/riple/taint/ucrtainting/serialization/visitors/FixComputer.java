package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CollectionHandler;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/**
 * General Fix visitor.This visitor determines the approach for resolving the error upon visiting
 * specific nodes that may impact the algorithm selection.
 */
public class FixComputer extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /** The javac context. */
  private final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  protected final Types types;
  protected final BasicVisitor basicVisitor;
  protected final SpecializedFixComputer thirdPartyFixVisitor;
  protected final SpecializedFixComputer methodTypeArgumentFixVisitor;

  public FixComputer(Context context, UCRTaintingAnnotatedTypeFactory factory) {
    this.context = context;
    this.typeFactory = factory;
    this.types = Types.instance(context);
    this.basicVisitor = new BasicVisitor(context, factory, this);
    this.thirdPartyFixVisitor = new ThirdPartyFixVisitor(context, typeFactory, this);
    this.methodTypeArgumentFixVisitor =
        new MethodTypeArgumentFixVisitor(context, typeFactory, this);
  }

  @Override
  public Set<Fix> defaultAction(Tree node, FoundRequired pair) {
    return node.accept(basicVisitor, pair);
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
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    if (typeFactory.isPresentInStub(node.getMethodSelect())) {
      return Set.of();
    }
    // Locate method receiver.
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    boolean isInAnnotatedPackage = Utility.isInAnnotatedPackage(calledMethod, typeFactory);
    boolean isTypeVar = Utility.containsTypeArgument(calledMethod.getReturnType());
    boolean hasReceiver =
        !(calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver));
    boolean hasPolyTaintedAnnotation =
        typeFactory.hasPolyTaintedAnnotation(calledMethod.getReturnType());
    boolean methodHasTypeArgs = !calledMethod.getTypeParameters().isEmpty();
    if (hasPolyTaintedAnnotation) {
      Set<Fix> polyFixes = node.accept(new PolyMethodVisitor(context, typeFactory, this), pair);
      if (!polyFixes.isEmpty()) {
        return polyFixes;
      }
    }
    if (CollectionHandler.isToArrayWithTypeArgMethod(calledMethod, types)) {
      return node.accept(new CollectionVisitor(context, typeFactory, this), pair);
    }
    if (methodHasTypeArgs) {
      return node.accept(methodTypeArgumentFixVisitor, pair);
    }
    // check if the call is to a method defined in a third party library. If the method has a type
    // var return type and has a receiver, we should annotate the receiver.
    if (!isInAnnotatedPackage && !(isTypeVar && hasReceiver)) {
      return node.accept(thirdPartyFixVisitor, pair);
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    if (isTypeVar && hasReceiver) {
      return node.accept(new ReceiverTypeArgumentFixVisitor(context, typeFactory, this), pair);
    } else {
      return defaultAction(node, pair);
    }
  }
}

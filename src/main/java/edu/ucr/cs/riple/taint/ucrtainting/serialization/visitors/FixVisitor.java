package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  private final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

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
   * Visitor for method invocations. For method invocations:
   *
   * <ol>
   *   <li>If return type is not type variable, we annotate the called method.
   *   <li>If return type is type variable and defined in source code, we annotate the called
   *       method.
   *   <li>If return type is type variable and defined in library, we annotate the receiver.
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
    // Locate method receiver.
    ExpressionTree receiver = null;
    if (node.getMethodSelect() instanceof MemberSelectTree) {
      receiver = ((MemberSelectTree) node.getMethodSelect()).getExpression();
    }
    boolean isInAnnotatedPackage = Utility.isInAnnotatedPackage(calledMethod, typeFactory);
    boolean isTypeVar = calledMethod.getReturnType() instanceof Type.TypeVar;
    boolean hasReceiver =
        !(calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver));
    boolean methodHasTypeArgs = !calledMethod.getTypeParameters().isEmpty();
    if (methodHasTypeArgs) {
      Set<Type.TypeVar> effectiveTypes = checkMethodTypeVarImpact(calledMethod, pair);
      if (!effectiveTypes.isEmpty()) {
        System.out.println("Method has type args");
        for (int i = 0; i < node.getArguments().size(); i++) {
          ExpressionTree passedArg = node.getArguments().get(i);
          Symbol.VarSymbol paramSymbol = calledMethod.getParameters().get(i);
          AnnotatedTypeMirror requiredParam =
              AnnotatedTypeMirror.createType(paramSymbol.type, typeFactory, true);
          System.out.println("EXCITING");
        }
      }
    }
    // check if the call is to a method defined in a third party library. If the method has a type
    // var return type and has a receiver, we should annotate the receiver.
    if (!isInAnnotatedPackage && !(isTypeVar && hasReceiver)) {
      return node.accept(new ThirdPartyFixVisitor(context, typeFactory), null);
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    if (isTypeVar && hasReceiver) {
      return node.accept(new TypeArgumentFixVisitor(context, typeFactory), unused);
    } else {
      return defaultAction(node, unused);
    }
  }

  private Set<Type.TypeVar> checkMethodTypeVarImpact(
      Symbol.MethodSymbol calledMethod, FoundRequired pair) {
    Set<Type.TypeVar> effectiveTypeVars = new HashSet<>();
    List<Symbol.TypeVariableSymbol> methodTypeVars = calledMethod.getTypeParameters();
    for (Symbol.TypeVariableSymbol methodTypeVar : methodTypeVars) {
      if (typeVarCanImpactFoundRequiredPair(
          (Type.TypeVar) methodTypeVar.type,
          calledMethod.getReturnType(),
          pair.found,
          pair.required)) {
        effectiveTypeVars.add((Type.TypeVar) methodTypeVar.type);
      }
    }
    return effectiveTypeVars;
  }

  /**
   * Checks if annotating the given type variable on the given type, can make the found type closer
   * to the required type.
   *
   * @param var The type variable to check.
   * @param type The type to check.
   * @param found The found type.
   * @param required The required type.
   * @return True if annotating the given type variable on the given type, can make the found type
   *     closer to the required type.
   */
  private boolean typeVarCanImpactFoundRequiredPair(
      Type.TypeVar var, Type type, AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    if (type instanceof Type.TypeVar) {
      return type.equals(var)
          && typeFactory.hasUntaintedAnnotation(required)
          && !typeFactory.hasUntaintedAnnotation(found);
    }
    if (type instanceof Type.ClassType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType foundDeclared =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) found;
      AnnotatedTypeMirror.AnnotatedDeclaredType requiredDeclared =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) required;
      Type.ClassType classType = (Type.ClassType) type;
      for (int i = 0; i < foundDeclared.getTypeArguments().size(); i++) {
        boolean canImpact =
            typeVarCanImpactFoundRequiredPair(
                var,
                classType.getTypeArguments().get(i),
                foundDeclared.getTypeArguments().get(i),
                requiredDeclared.getTypeArguments().get(i));
        if (canImpact) {
          return true;
        }
      }
    }
    return false;
  }
}

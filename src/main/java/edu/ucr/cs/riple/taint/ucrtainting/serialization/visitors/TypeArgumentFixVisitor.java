package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.google.common.base.Preconditions;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

public class TypeArgumentFixVisitor extends BasicVisitor {

  /**
   * The list method invocations that their return type contained a type argument. Used to detect
   * which type in the receiver should be annotated.
   */
  private List<ExpressionTree> receivers;

  public TypeArgumentFixVisitor(Context context, UCRTaintingAnnotatedTypeFactory factory) {
    super(context, factory, null);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Void unused) {
    if (typeFactory.mayBeTainted(node.getExpression())) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }
      // If fix on receiver, we should annotate type parameter that matches the target type.
      if (Utility.isFullyParameterizedType(((Symbol) member).type)) {
        // If is a parameterized type, then we found the right declaration.
        Fix fix = buildFixForElement(TreeUtils.elementFromUse(node));
        return fix == null ? Set.of() : Set.of(fix);
      } else if (node instanceof JCTree.JCFieldAccess) {
        // Need to traverse the tree to find the right declaration.
        JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
        addReceiver(fieldAccess);
        return fieldAccess.selected.accept(this, unused);
      }
    }
    return Set.of();
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
    // If method is static, or has no receiver, or receiver is "this", we must annotate the method
    // directly.
    if (calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver)) {
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
    }
    if (Utility.isFullyParameterizedType(calledMethod.getReturnType())) {
      // Found the right declaration.
      addReceiver(node);
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
    }
    addReceiver(node);
    return receiver.accept(this, unused);
  }

  /**
   * Adds the given tree to the list of receivers.
   *
   * @param tree The tree to add to the list of receivers.
   */
  private void addReceiver(ExpressionTree tree) {
    if (this.receivers == null) {
      this.receivers = new ArrayList<>();
    }
    this.receivers.add(0, tree);
  }

  /**
   * Returns the list of type arguments for the given element.
   *
   * @param elementType The element to get the type arguments for.
   * @return The list of type arguments for the given element.
   */
  private List<Type> getAllTypeArguments(Type elementType) {
    if (elementType instanceof Type.ClassType) {
      return (elementType).tsym.type.allparams();
    } else {
      return elementType.tsym.type.getTypeArguments();
    }
  }

  /**
   * Returns the list of type arguments for the given element.
   *
   * @param element The element to get the type arguments for.
   * @return The list of type arguments for the given element.
   */
  private List<Type> getProvidedTypeArguments(Element element) {
    Symbol symbol = (Symbol) element;
    Type type =
        symbol instanceof Symbol.MethodSymbol
            ? ((Symbol.MethodSymbol) symbol).getReturnType()
            : symbol.type;
    if (type instanceof Type.ClassType) {
      return type.allparams();
    }
    return type.getTypeArguments();
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element) {
    Fix fix = super.buildFixForElement(element);
    List<Integer> indexes = locateTheEffectiveTypeParameter(element);
    return fix;
  }

  /**
   * Locates the type parameter in the declaration based on the chain of receivers.
   *
   * @param element The element which provided the type parameters.
   * @return The list of indexes of the type parameters.
   */
  private List<Integer> locateTheEffectiveTypeParameter(Element element) {
    Type elementType =
        element instanceof Symbol.MethodSymbol
            ? ((Symbol.MethodSymbol) element).getReturnType()
            : ((Symbol) element).type;
    List<Integer> indexes = new ArrayList<>();
    // Indexes of the type variables to locate the type which needs to be modified.
    Map<Type.TypeVar, Type.TypeVar> typeVarMap = new HashMap<>();
    List<Type> elementTypeArgs = getAllTypeArguments(elementType);
    getAllTypeArguments(elementType)
        .forEach(
            type -> {
              Preconditions.checkArgument(type instanceof Type.TypeVar);
              typeVarMap.put((Type.TypeVar) type, (Type.TypeVar) type);
            });
    for (ExpressionTree receiver : receivers) {
      // Locate passed type arguments
      Symbol receiverSymbol = (Symbol) TreeUtils.elementFromUse(receiver);
      Type receiverType =
          receiverSymbol instanceof Symbol.MethodSymbol
              ? ((Symbol.MethodSymbol) receiverSymbol).getReturnType()
              : receiverSymbol.type;
      List<Type> providedTypeArgsForReceiver = receiverType.getTypeArguments();

      // Update translation:
      List<Type> typeArguments = getAllTypeArguments(receiverType);
      for (int i = 0; i < providedTypeArgsForReceiver.size(); i++) {
        Type providedI = providedTypeArgsForReceiver.get(i);
        if (!(providedI instanceof Type.TypeVar)) {
          continue;
        }
        Type.TypeVar provided = (Type.TypeVar) providedI;
        if (typeVarMap.containsKey(provided)) {
          Type.TypeVar value = typeVarMap.get(provided);
          typeVarMap.remove(provided);
          typeVarMap.put((Type.TypeVar) typeArguments.get(i), value);
        }
      }

      if (receiverType instanceof Type.TypeVar) {
        // We should refresh base.
        Type.TypeVar original = typeVarMap.get((Type.TypeVar) (receiverType));
        int i;
        for (i = 0; i < elementTypeArgs.size(); i++) {
          if (elementTypeArgs.get(i).equals(original)) {
            indexes.add(i);
            break;
          }
        }
        elementType = getProvidedTypeArguments(element).get(i);
        elementTypeArgs = getAllTypeArguments(elementType);
        typeVarMap.clear();
        getAllTypeArguments(elementType)
            .forEach(
                type -> {
                  Preconditions.checkArgument(type instanceof Type.TypeVar);
                  typeVarMap.put((Type.TypeVar) type, (Type.TypeVar) type);
                });
      }
    }
    return indexes;
  }
}

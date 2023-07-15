package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getAnnotatedTypeMirrorOfTypeArgumentAt;
import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class ReceiverTypeParameterFixVisitor extends BasicVisitor {

  /**
   * The list method invocations that their return type contained a type argument. Used to detect
   * which type in the receiver should be annotated.
   */
  private List<ExpressionTree> receivers;

  public ReceiverTypeParameterFixVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    super(context, factory, pair);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Void unused) {
    addReceiver(node);
    Fix fix = buildFixForElement(TreeUtils.elementFromTree(node));
    return fix == null ? Set.of() : Set.of(fix);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Void unused) {
    Element member = TreeUtils.elementFromUse(node);
    if (!(member instanceof Symbol)) {
      return Set.of();
    }
    // Check if receiver is used as raw type. In this case, we should annotate the called method.
    if (Utility.elementHasRawType(member)) {
      if (!receivers.isEmpty()) {
        return receivers
            .get(receivers.size() - 1)
            .accept(new BasicVisitor(context, typeFactory, null), null);
      }
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
    return Set.of();
  }

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
    // Check if receiver is used as raw type. In this case, we should annotate the called method.
    if (Utility.elementHasRawType(calledMethod)) {
      if (!receivers.isEmpty()) {
        return receivers
            .get(receivers.size() - 1)
            .accept(new BasicVisitor(context, typeFactory, null), null);
      }
    }
    addReceiver(node);
    if (Utility.isFullyParameterizedType(calledMethod.getReturnType())) {
      // Found the right declaration.
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
    }
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
   * Returns the list of type arguments of the for the given element's type (e.g. {@code
   * List<String> will return E}).
   *
   * @param elementType The element to get the type arguments for.
   * @return The list of type arguments for the given element.
   */
  private List<Type.TypeVar> getAllTypeArguments(Type elementType) {
    List<Type> typeArgsList =
        elementType instanceof Type.ClassType
            // Should return all type arguments, including those of the outer class.
            ? elementType.tsym.type.allparams()
            : elementType.tsym.type.getTypeArguments();
    // Should return as list to preserve the order of the type arguments.
    return typeArgsList.stream().map(type -> (Type.TypeVar) type).collect(Collectors.toList());
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    if (getType(element).allparams().isEmpty()) {
      // receiver is written as a raw type and not parameterized. We cannot infer the actual types
      // and have to annotate the method directly.
      Set<Fix> fixes =
          receivers
              .get(receivers.size() - 1)
              .accept(new BasicVisitor(context, typeFactory, null), null);
      if (fixes != null && !fixes.isEmpty()) {
        return fixes.iterator().next();
      }
    }
    List<List<Integer>> indexes = locateEffectiveTypeParameter(element);
    if (!indexes.isEmpty()) {
      location.setTypeVariablePositions(indexes);
    }
    return new Fix("untainted", location);
  }

  /**
   * Locates the type parameter in the declaration based on the chain of receivers.
   *
   * @param element The element which provided the type parameters.
   * @return The list of indexes of the type parameters.
   */
  private List<List<Integer>> locateEffectiveTypeParameter(Element element) {
    Type elementParameterizedType = getType(element);
    Type base = elementParameterizedType;
    // Indexes of the type variables to locate the type which needs to be modified.
    List<Integer> indexes = new ArrayList<>();
    // Map of type arguments symbol names to their provided type parameters symbol names. Cannot use
    // a map of <Type.TypeVar, Type.TypeVar> since 2 variables with same name can have different
    // owners and are not considered equal.
    Map<String, String> typeVarMap = new HashMap<>();
    List<Type.TypeVar> elementTypeArgs = getAllTypeArguments(elementParameterizedType);
    getAllTypeArguments(elementParameterizedType)
        .forEach(type -> typeVarMap.put(type.toString(), type.toString()));
    for (ExpressionTree receiver : receivers) {
      // Locate passed type arguments
      Type receiverParameterizedType = getType(TreeUtils.elementFromUse(receiver));
      List<Type> receiverProvidedParameterTypes = receiverParameterizedType.getTypeArguments();

      // Update translation:
      List<Type.TypeVar> typeArgumentsForReceiver = getAllTypeArguments(receiverParameterizedType);
      Set<String> existingTypeVars = typeVarMap.keySet();
      Set<String> toRemove = new HashSet<>();
      for (int i = 0; i < receiverProvidedParameterTypes.size(); i++) {
        Type providedI = receiverProvidedParameterTypes.get(i);
        if (!(providedI instanceof Type.TypeVar)) {
          continue;
        }
        String provided = providedI.toString();
        if (existingTypeVars.contains(provided)) {
          String value = typeVarMap.get(provided);
          String newKey = typeArgumentsForReceiver.get(i).toString();
          if (!provided.equals(newKey)) {
            toRemove.add(provided);
            typeVarMap.put(newKey, value);
          }
        }
      }
      toRemove.forEach(typeVarMap::remove);
      // Check if we entered inside a type parameter. (e.g. E or Foo<E>)
      if (receiverParameterizedType instanceof Type.TypeVar
          || typeArgumentsForReceiver.size() == 1) {
        String enteredType =
            receiverParameterizedType instanceof Type.TypeVar
                ? receiverParameterizedType.toString()
                : typeArgumentsForReceiver.get(0).toString();
        // We should refresh base.
        String original = typeVarMap.get(enteredType);
        if (original == null) {
          // The remaining inconsistencies are due to the fact that the parameters are not inside
          // the receiver. We should locate the remaining. See example below: Iterator<Entry<String,
          // String>> itEntries = null; @RUntainted Entry<@RUntainted String, @RUntainted String>
          // entry = itEntries.next(); With controlling type argument, we can make result of next()
          // untainted. However, we need to make the including type args of Entry untainted as well.
          List<List<Integer>> rest;
          AnnotatedTypeMirror m = typeFactory.getAnnotatedType(element);
          AnnotatedTypeMirror.AnnotatedDeclaredType type =
              (AnnotatedTypeMirror.AnnotatedDeclaredType) m;
          AnnotatedTypeMirror foundOnTypeArg =
              getAnnotatedTypeMirrorOfTypeArgumentAt(type, new ArrayDeque<>(indexes));
          rest = new TypeMatchVisitor(typeFactory).visit(foundOnTypeArg, pair.required, null);
          List<List<Integer>> positions;
          if (!rest.isEmpty()) {
            positions = new ArrayList<>();
            for (List<Integer> integers : rest) {
              List<Integer> position = new ArrayList<>(indexes);
              position.addAll(integers);
              positions.add(position);
            }
            return positions;
          } else {
            return List.of(indexes);
          }
        }
        int i;
        for (i = 0; i < elementTypeArgs.size(); i++) {
          if (elementTypeArgs.get(i).toString().equals(original)) {
            indexes.add(i + 1);
            break;
          }
        }
        elementParameterizedType = base.allparams().get(i);
        base = elementParameterizedType;
        elementTypeArgs = getAllTypeArguments(elementParameterizedType);
        if (elementTypeArgs.size() == 0) {
          // Reached the end of type parameters.
          break;
        }
        typeVarMap.clear();
        getAllTypeArguments(elementParameterizedType)
            .forEach(type -> typeVarMap.put(type.toString(), type.toString()));
      }
    }
    if (!indexes.isEmpty()) {
      indexes.add(0);
    }
    return List.of(indexes);
  }
}

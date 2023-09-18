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
import com.sun.tools.javac.util.Pair;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class ReceiverTypeArgumentFixVisitor extends SpecializedFixComputer {

  /**
   * The list method invocations that their return type contained a type argument. Used to detect
   * which type in the receiver should be annotated.
   */
  protected List<ExpressionTree> receivers;

  public ReceiverTypeArgumentFixVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer) {
    super(context, factory, fixComputer);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    addReceiver(node);
    Fix fix = buildFixForElement(TreeUtils.elementFromTree(node), pair);
    return fix == null ? Set.of() : Set.of(fix);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
    Element member = TreeUtils.elementFromUse(node);
    if (!(member instanceof Symbol)) {
      return Set.of();
    }
    // Check if receiver is used as raw type. In this case, we should annotate the called method.
    if (Utility.elementHasRawType(member)) {
      if (!receivers.isEmpty()) {
        return receivers.get(receivers.size() - 1).accept(fixComputer.basicVisitor, pair);
      }
    }
    // If fix on receiver, we should annotate type parameter that matches the target type.
    if (Utility.isFullyParameterizedType(((Symbol) member).type)) {
      // If is a parameterized type, then we found the right declaration.
      Fix fix = buildFixForElement(TreeUtils.elementFromUse(node), pair);
      return fix == null ? Set.of() : Set.of(fix);
    } else if (node instanceof JCTree.JCFieldAccess) {
      // Need to traverse the tree to find the right declaration.
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
      addReceiver(fieldAccess);
      return fieldAccess.selected.accept(this, pair);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
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
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod, pair)));
    }
    // Check if receiver is used as raw type. In this case, we should annotate the called method.
    if (Utility.elementHasRawType(calledMethod)) {
      if (!receivers.isEmpty()) {
        return receivers.get(receivers.size() - 1).accept(fixComputer.basicVisitor, pair);
      }
    }
    addReceiver(node);
    if (Utility.isFullyParameterizedType(calledMethod.getReturnType())) {
      // Found the right declaration.
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod, pair)));
    }
    return receiver.accept(this, pair);
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
   * Returns the list of type variables of the for the given element's type (e.g. {@code
   * List<String> will return E}).
   *
   * @param elementType The element to get the type arguments for.
   * @return The list of type arguments for the given element.
   */
  private List<Type.TypeVar> getTypeVariables(Type elementType) {
    List<Type> typeArgsList =
        elementType instanceof Type.ClassType
            // Should return all type arguments, including those of the outer class.
            ? elementType.tsym.type.allparams()
            : elementType.tsym.type.getTypeArguments();
    // Should return as list to preserve the order of the type variables.
    return typeArgsList.stream().map(type -> (Type.TypeVar) type).collect(Collectors.toList());
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    if (getType(element).allparams().isEmpty()) {
      // receiver is written as a raw type and not parameterized. We cannot infer the actual types
      // and have to annotate the method directly.
      Set<Fix> fixes = receivers.get(receivers.size() - 1).accept(fixComputer.basicVisitor, null);
      if (fixes != null && !fixes.isEmpty()) {
        return fixes.iterator().next();
      }
    }
    List<List<Integer>> indexes = computeIndices(element, typeMatchVisitor, pair);
    location.setTypeVariablePositions(indexes);
    return new Fix(location);
  }

  /**
   * Locates the type parameter in the declaration based on the chain of receivers.
   *
   * @param element The element which provided the type parameters.
   * @return The list of indexes of the type parameters.
   */
  protected List<List<Integer>> computeIndices(
      Element element, TypeMatchVisitor visitor, FoundRequired pair) {
    // Indexes of the effective type argument based on the chain of receivers.
    Pair<List<Integer>, Map<String, String>> effectiveRegion =
        locateEffectiveTypeArgumentRegion(element);
    List<Integer> effectiveRegionIndex = effectiveRegion.fst;
    Map<String, String> typeVarMap = effectiveRegion.snd;
    return matchOnCurrentType(element, visitor, effectiveRegionIndex, pair, typeVarMap);
  }

  private Pair<List<Integer>, Map<String, String>> locateEffectiveTypeArgumentRegion(
      Element element) {
    Type elementParameterizedType = getType(element);
    Type base = elementParameterizedType;
    // Indexes of the effective type argument based on the chain of receivers.
    List<Integer> effectiveRegionIndex = new ArrayList<>();
    // Map of type variables names to their provided type argument symbol names. Cannot use
    // a map of <Type.TypeVar, Type.TypeVar> since 2 variables with same name can have different
    // owners and are not considered equal.
    List<Type.TypeVar> elementTypeVariables = getTypeVariables(elementParameterizedType);
    Map<String, String> typeVarMap =
        elementTypeVariables.stream()
            .collect(Collectors.toMap(Type.TypeVar::toString, Type.TypeVar::toString));
    for (ExpressionTree receiver : receivers) {
      Type receiverType = getType(TreeUtils.elementFromUse(receiver));
      // Extract passed type arguments in a list ordered by the declaration.
      List<Type> receiverTypeArguments = receiverType.getTypeArguments();
      // Update mapping. (e.g. M and N can be provided to Map<M, N>), however, inside the maps,
      // their actual names are K, V. Should update the mapping to reflect that.
      // Get the defined type arguments for the receiver.
      List<Type.TypeVar> receiverTypeVariables = getTypeVariables(receiverType);
      Set<String> existingTypeVars = typeVarMap.keySet();
      Set<String> toRemove = new HashSet<>();
      for (int i = 0; i < receiverTypeArguments.size(); i++) {
        Type providedAtIndexI = receiverTypeArguments.get(i);
        if (!(providedAtIndexI instanceof Type.TypeVar)) {
          // Not a type variable, we do not need to update. Example for String in Foo<T, String>.
          continue;
        }
        String providedTypeArgName = providedAtIndexI.toString();
        if (existingTypeVars.contains(providedTypeArgName)) {
          String newKey = receiverTypeVariables.get(i).toString();
          if (!providedTypeArgName.equals(newKey)) {
            toRemove.add(providedTypeArgName);
            typeVarMap.put(newKey, typeVarMap.get(providedTypeArgName));
          }
        }
      }
      toRemove.forEach(typeVarMap::remove);
      // Check if we entered inside a type parameter. (e.g. E or Foo<E>)
      if (receiverType instanceof Type.TypeVar || receiverTypeVariables.size() == 1) {
        String enteredType =
            receiverType instanceof Type.TypeVar
                ? receiverType.toString()
                : receiverTypeVariables.get(0).toString();
        // We should refresh our map since we no longer are inside the receiver. From now on we are
        // inside a provided type parameter.
        String enteredName = typeVarMap.get(enteredType);
        if (enteredName == null) {
          break;
        }
        int i;
        for (i = 0; i < elementTypeVariables.size(); i++) {
          if (elementTypeVariables.get(i).toString().equals(enteredName)) {
            effectiveRegionIndex.add(i + 1);
            break;
          }
        }
        elementParameterizedType = base.allparams().get(i);
        base = elementParameterizedType;
        elementTypeVariables = getTypeVariables(elementParameterizedType);
        if (elementTypeVariables.isEmpty()) {
          // Reached the end of type parameters.
          typeVarMap.clear();
          break;
        }
        // Update the map with entered type's type arguments.
        typeVarMap =
            elementTypeVariables.stream()
                .collect(Collectors.toMap(Type.TypeVar::toString, Type.TypeVar::toString));
      }
    }
    return Pair.of(effectiveRegionIndex, typeVarMap);
  }

  private List<List<Integer>> matchOnCurrentType(
      Element element,
      TypeMatchVisitor visitor,
      List<Integer> effectiveRegionIndex,
      FoundRequired pair,
      Map<String, String> typeVarMap) {
    // The remaining inconsistencies are due to the fact that the parameters are not inside
    // the receiver. We should locate the remaining. See example below:
    // Iterator<Entry<String, String>> itEntries;
    // @RUntainted Entry<@RUntainted String, @RUntainted String> entry = itEntries.next();
    // With controlling type argument, we can make result of next()
    // untainted. However, we need to make the including type args of Entry untainted as well.
    AnnotatedTypeMirror typeArgumentRegion =
        getAnnotatedTypeMirrorOfTypeArgumentAt(
            typeFactory.getAnnotatedType(element), effectiveRegionIndex);
    if (typeArgumentRegion instanceof AnnotatedTypeMirror.AnnotatedExecutableType) {
      typeArgumentRegion =
          ((AnnotatedTypeMirror.AnnotatedExecutableType) typeArgumentRegion).getReturnType();
    }
    List<List<Integer>> positions = new ArrayList<>();
    if (!(typeArgumentRegion instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)
        || !(pair.found instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      List<List<Integer>> result = visitor.visit(typeArgumentRegion, pair.required, null);
      if (!result.isEmpty()) {
        for (List<Integer> integers : result) {
          List<Integer> position = new ArrayList<>(effectiveRegionIndex);
          position.addAll(integers);
          positions.add(position);
        }
      }
      return positions;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType regionDeclaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) typeArgumentRegion;
    AnnotatedTypeMirror.AnnotatedDeclaredType requiredDeclared =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.required;
    TypeMirror foundType = typeArgumentRegion.getUnderlyingType();
    TypeMirror requiredType = pair.required.getUnderlyingType();
    if (!(foundType instanceof Type.ClassType) || !(requiredType instanceof Type.ClassType)) {
      throw new RuntimeException("Not a class type");
    }
    if (typeVarMap.isEmpty()) {
      AnnotatedTypeMirror requiredTypeArgument;
      if (requiredDeclared.getTypeArguments().isEmpty()) {
        requiredTypeArgument = requiredDeclared;
      } else if (requiredDeclared.getTypeArguments().size() == 1) {
        requiredTypeArgument = requiredDeclared.getTypeArguments().get(0);
      } else {
        throw new RuntimeException("Empty type var map");
      }
      List<List<Integer>> result = visitor.visit(regionDeclaredType, requiredTypeArgument, null);
      if (!result.isEmpty()) {
        for (List<Integer> integers : result) {
          List<Integer> position = new ArrayList<>(effectiveRegionIndex);
          position.addAll(integers);
          positions.add(position);
        }
      }
      return positions;
    }
    if (typeFactory.hasUntaintedAnnotation(requiredDeclared)
        && !typeFactory.hasUntaintedAnnotation(regionDeclaredType)) {
      List<Integer> onType = new ArrayList<>(effectiveRegionIndex);
      onType.add(0);
      positions.add(onType);
    }
    Type.ClassType foundClass = (Type.ClassType) foundType;
    Type.ClassType requiredClass = (Type.ClassType) requiredType;
    List<String> foundTypeVariables =
        foundClass.tsym.type.getTypeArguments().stream()
            .map(Type::toString)
            .collect(Collectors.toList());
    List<String> requiredTypeVariables =
        requiredClass.tsym.type.getTypeArguments().stream()
            .map(Type::toString)
            .collect(Collectors.toList());
    for (int i = 0; i < requiredTypeVariables.size(); i++) {
      int regionTypeVariableIndex =
          foundTypeVariables.indexOf(typeVarMap.get(requiredTypeVariables.get(i)));
      AnnotatedTypeMirror regionTypeArgument =
          regionDeclaredType.getTypeArguments().get(regionTypeVariableIndex);
      AnnotatedTypeMirror requiredTypeArgument = requiredDeclared.getTypeArguments().get(i);
      List<List<Integer>> result = visitor.visit(regionTypeArgument, requiredTypeArgument, null);
      if (!result.isEmpty()) {
        for (List<Integer> integers : result) {
          List<Integer> position = new ArrayList<>(effectiveRegionIndex);
          position.addAll(integers);
          position.add(0, i + 1);
          positions.add(position);
        }
      }
    }
    return positions;
  }
}

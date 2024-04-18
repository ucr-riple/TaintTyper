package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class ReceiverTypeArgumentFixVisitor extends SpecializedFixComputer {

  public ReceiverTypeArgumentFixVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Fix fix = buildFixForElement(TreeUtils.elementFromTree(node), pair);
    return fix == null ? Set.of() : Set.of(fix);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
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
    ExpressionTree receiver = TreeUtils.getReceiverTree(node);
    // If method is static, or has no receiver, or receiver is "this", we must annotate the method
    // directly.
    if (calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver)) {
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod, pair)));
    }
    FoundRequired f = computeRequiredTypeForReceiverMatchingTypeArguments(node, receiver, pair, calledMethod.getReturnType());
    return receiver.accept(this, f);
  }

  private FoundRequired computeRequiredTypeForReceiverMatchingTypeArguments(
      ExpressionTree node, ExpressionTree receiver, FoundRequired pair, Type d) {
    AnnotatedTypeMirror invocationAnnotatedType = typeFactory.getAnnotatedType(node);
    List<List<Integer>> differences =
        typeMatchVisitor.visit(invocationAnnotatedType, pair.required, null);
    // Based on the differences above, we need to find the required type of each type argument
    Map<Type.TypeVar, List<List<Integer>>> involvedTypeVariables = new HashMap<>();
    differences.forEach(
        integers -> {
          Deque<Integer> deque = new ArrayDeque<>(integers);
          Type declaredType = d;
          while (!deque.isEmpty()) {
            if (declaredType instanceof Type.TypeVar) {
              if (!involvedTypeVariables.containsKey(declaredType)) {
                List<List<Integer>> ans = new ArrayList<>();
                List<Integer> list = new ArrayList<>(deque);
                ans.add(list);
                involvedTypeVariables.put((Type.TypeVar) declaredType, ans);
              } else {
                involvedTypeVariables.get(declaredType).add(new ArrayList<>(deque));
              }
              break;
            } else {
              int index = deque.poll() - 1;
              declaredType = declaredType.allparams().get(index);
            }
          }
        });
    Type receiverType = getType(receiver);
    AnnotatedTypeMirror receiverAnnotatedType = typeFactory.getAnnotatedType(receiver);
    if (!(receiverAnnotatedType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return null;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType receiverDeclaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) receiverAnnotatedType.deepCopy(true);
    involvedTypeVariables.forEach(
        (typeVar, lists) -> {
          int i = receiverType.tsym.type.getTypeArguments().indexOf(typeVar);
          if (i == -1) {
            return;
          }
          AnnotatedTypeMirror typeArgumentType = receiverDeclaredType.getTypeArguments().get(i);
          typeFactory.makeUntainted(typeArgumentType, lists);
        });
    return FoundRequired.of(receiverAnnotatedType, receiverDeclaredType, pair.depth);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
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
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class ReceiverTypeArgumentFixVisitor extends SpecializedFixComputer {

  Types types;

  public ReceiverTypeArgumentFixVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    types = Types.instance(context);
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Fix fix = buildFixForElement(TreeUtils.elementFromTree(node), pair);
    return fix == null ? Set.of() : Set.of(fix);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, FoundRequired pair) {
    JCTree declaration =
        Utility.locateDeclaration((Symbol) TreeUtils.elementFromUse(node), context);
    if (declaration == null) {
      return node.accept(fixComputer, pair);
    }
    Type declarationType = declaration.type;
    if (declarationType == null && declaration instanceof JCTree.JCVariableDecl) {
      declarationType = ((JCTree.JCVariableDecl) declaration).vartype.type;
    }
    if (Utility.isFullyParameterizedType(declarationType)) {
      return node.accept(fixComputer, pair);
    }
    FoundRequired f =
        computeRequiredTypeForReceiverMatchingTypeArguments(
            node, node.getExpression(), pair, declarationType);
    return node.getExpression().accept(this, f);
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
    // Locate the declaration of the method.
    JCTree declaration = Utility.locateDeclaration(calledMethod, context);
    if (declaration instanceof JCTree.JCMethodDecl) {
      Type returnType = ((JCTree.JCMethodDecl) declaration).restype.type;
      if (Utility.isFullyParameterizedType(returnType)) {
        return node.accept(fixComputer, pair);
      }
    }
    FoundRequired f =
        computeRequiredTypeForReceiverMatchingTypeArguments(
            node, receiver, pair, calledMethod.getReturnType());
    return receiver.accept(this, f);
  }

  private FoundRequired computeRequiredTypeForReceiverMatchingTypeArguments(
      ExpressionTree node, ExpressionTree receiver, FoundRequired pair, Type typeOnDeclaration) {
    AnnotatedTypeMirror expressionAnnotatedType = typeFactory.getAnnotatedType(node);
    AnnotatedTypeMirror receiverAnnotatedType = typeFactory.getAnnotatedType(receiver);
    if (!(receiverAnnotatedType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return null;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType receiverDeclaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) receiverAnnotatedType.deepCopy(true);
    AnnotatedTypeMirror currentExpressionAnnotatedType = expressionAnnotatedType;
    AnnotatedTypeMirror currentRequiredType = pair.required;
    while (currentExpressionAnnotatedType != null && currentRequiredType != null) {
      List<List<Integer>> differences =
          typeMatchVisitor.visit(currentExpressionAnnotatedType, currentRequiredType, null);
      // Based on the differences above, we need to find the required type of each type argument
      Map<String, List<List<Integer>>> involvedTypeVariables = new HashMap<>();
      differences.forEach(
          integers -> {
            Deque<Integer> deque = new ArrayDeque<>(integers);
            Type declaredType = typeOnDeclaration;
            while (!deque.isEmpty()) {
              if (declaredType instanceof Type.TypeVar) {
                String typeVarName = declaredType.tsym.name.toString();
                if (!involvedTypeVariables.containsKey(typeVarName)) {
                  List<List<Integer>> ans = new ArrayList<>();
                  List<Integer> list = new ArrayList<>(deque);
                  ans.add(list);
                  involvedTypeVariables.put(typeVarName, ans);
                } else {
                  involvedTypeVariables.get(typeVarName).add(new ArrayList<>(deque));
                }
                break;
              } else {
                int index = deque.poll() - 1;
                if (index < 0) {
                  break;
                }
                declaredType = declaredType.allparams().get(index);
              }
            }
          });
      Type receiverType = getType(receiver);
      List<String> typeVariablesNameInReceiver =
          Utility.getTypeVariables(receiverType).stream()
              .map(t -> t.tsym.name.toString())
              .collect(Collectors.toList());
      List<AnnotatedTypeMirror> allTypeArguments =
          new ArrayList<>(receiverDeclaredType.getTypeArguments());
      if (receiverDeclaredType.getEnclosingType() != null
          && receiverDeclaredType.getEnclosingType().getTypeArguments() != null) {
        allTypeArguments.addAll(0, receiverDeclaredType.getEnclosingType().getTypeArguments());
      }
      involvedTypeVariables.forEach(
          (typeVarName, lists) -> {
            int i = typeVariablesNameInReceiver.indexOf(typeVarName);
            if (i == -1) {
              return;
            }
            AnnotatedTypeMirror typeArgumentType = allTypeArguments.get(i);
            typeFactory.makeUntainted(typeArgumentType, lists);
            System.out.println();
          });
      if (currentExpressionAnnotatedType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
        currentExpressionAnnotatedType =
            ((AnnotatedTypeMirror.AnnotatedDeclaredType) currentExpressionAnnotatedType)
                .getEnclosingType();
      } else {
        currentExpressionAnnotatedType = null;
      }
      if (currentRequiredType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
        currentRequiredType =
            ((AnnotatedTypeMirror.AnnotatedDeclaredType) currentRequiredType).getEnclosingType();
      } else {
        currentRequiredType = null;
      }
    }
    return FoundRequired.of(receiverAnnotatedType, receiverDeclaredType, pair.depth);
  }
}

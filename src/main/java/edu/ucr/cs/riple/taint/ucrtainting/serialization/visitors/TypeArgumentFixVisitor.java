package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

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
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import edu.ucr.cs.riple.taint.ucrtainting.util.TypeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TreeUtils;

public class TypeArgumentFixVisitor extends SpecializedFixComputer {

  public TypeArgumentFixVisitor(
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
    JCTree declaration =
        SymbolUtils.locateDeclaration((Symbol) TreeUtils.elementFromUse(node), context);
    if (declaration == null) {
      // If we cannot locate the declaration, we cannot suggest a fix on its type arguments.
      return Set.of();
    }
    Type declarationType = declaration.type;
    if (declarationType == null && declaration instanceof JCTree.JCVariableDecl) {
      declarationType = ((JCTree.JCVariableDecl) declaration).vartype.type;
    }
    if (TypeUtils.isFullyParameterizedType(declarationType)) {
      // Element is fully parameterized, no need to update the required type argument, we should
      // directly fix the declaration.
      return node.accept(fixComputer, pair);
    }
    Element receiverElement = TreeUtils.elementFromUse(node.getExpression());
    if (TypeUtils.elementHasRawType(receiverElement)) {
      // Receiver is raw type, no fix can be suggested by this visitor.
      return Set.of();
    }
    // Receiver is not parameterized and has type arguments, we need to update the required type
    // argument.
    FoundRequired updatedFoundRequiredPair = translateToReceiverRequiredPair(node, pair);
    if (updatedFoundRequiredPair == null) {
      return Set.of();
    }
    return node.getExpression().accept(this, updatedFoundRequiredPair);
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
    // static method return type cannot be changed using class type arguments. No fix can be
    // suggested by this visitor.
    if (calledMethod.isStatic()) {
      return Set.of();
    }
    // If receiver is null or a simple identifier, we must fix the method declaration.
    if (receiver == null || SymbolUtils.isThisIdentifier(receiver)) {
      return node.accept(fixComputer, pair);
    }
    Element receiverElement = TreeUtils.elementFromUse(receiver);
    if (TypeUtils.elementHasRawType(receiverElement)) {
      // Receiver is raw type, no fix can be suggested by this visitor.
      return Set.of();
    }
    // Locate the declaration of the method.
    JCTree declaration = SymbolUtils.locateDeclaration(calledMethod, context);
    if (declaration instanceof JCTree.JCMethodDecl) {
      Type returnType = ((JCTree.JCMethodDecl) declaration).restype.type;
      if (TypeUtils.isFullyParameterizedType(returnType)) {
        return node.accept(fixComputer, pair);
      }
    }
    FoundRequired updatedFoundRequiredPair = translateToReceiverRequiredPair(node, pair);
    if (updatedFoundRequiredPair == null) {
      return Set.of();
    }
    return receiver.accept(this, updatedFoundRequiredPair);
  }

  /**
   * Translate the required type of the expression to the required type of the receiver.
   *
   * @param expr The expression tree.
   * @param pair The required type of the expression.
   * @return The required type of the receiver.
   */
  @Nullable
  private FoundRequired translateToReceiverRequiredPair(ExpressionTree expr, FoundRequired pair) {
    Type formalDeclaredType = TypeUtils.getType(TreeUtils.elementFromTree(expr));
    ExpressionTree receiver = TreeUtils.getReceiverTree(expr);
    AnnotatedTypeMirror expressionAnnotatedType = typeFactory.getAnnotatedType(expr);
    AnnotatedTypeMirror receiverAnnotatedType = typeFactory.getAnnotatedType(receiver);
    if (!(receiverAnnotatedType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return null;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType receiverDeclaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) receiverAnnotatedType.deepCopy(true);
    AnnotatedTypeMirror currentExpressionAnnotatedType = expressionAnnotatedType;
    AnnotatedTypeMirror currentRequiredType = pair.required;
    while (currentExpressionAnnotatedType != null && currentRequiredType != null) {
      Set<TypeIndex> differences =
          untaintedTypeMatchVisitor.visit(
              currentExpressionAnnotatedType, currentRequiredType, null);
      // Based on the differences above, we need to find the required type of each type argument
      Map<String, Set<TypeIndex>> involvedTypeVariables = new HashMap<>();
      differences.forEach(
          typeIndex -> {
            TypeIndex copy = typeIndex.copy();
            Type declaredType = formalDeclaredType;
            while (!copy.isEmpty()) {
              if (declaredType instanceof Type.TypeVar) {
                String typeVarName = declaredType.tsym.name.toString();
                if (!involvedTypeVariables.containsKey(typeVarName)) {
                  involvedTypeVariables.put(typeVarName, TypeIndex.setOf(copy));
                } else {
                  involvedTypeVariables.get(typeVarName).add(copy);
                }
                break;
              } else {
                int index = copy.poll() - 1;
                if (index < 0) {
                  break;
                }
                declaredType = declaredType.allparams().get(index);
              }
            }
          });
      Type receiverType = TypeUtils.getType(receiver);
      List<String> typeVariablesNameInReceiver =
          TypeUtils.getTypeVariables(receiverType).stream()
              .map(t -> t.tsym.name.toString())
              .collect(Collectors.toList());
      List<AnnotatedTypeMirror> allTypeArguments =
          new ArrayList<>(receiverDeclaredType.getTypeArguments());
      if (receiverDeclaredType.getEnclosingType() != null
          && receiverDeclaredType.getEnclosingType().getTypeArguments() != null) {
        allTypeArguments.addAll(0, receiverDeclaredType.getEnclosingType().getTypeArguments());
      }
      for (Map.Entry<String, Set<TypeIndex>> entry : involvedTypeVariables.entrySet()) {
        String typeVarName = entry.getKey();
        Set<TypeIndex> lists = entry.getValue();
        int i = typeVariablesNameInReceiver.indexOf(typeVarName);
        if (i == -1) {
          Symbol nodeSymbol = (Symbol) TreeUtils.elementFromTree(expr);
          if (nodeSymbol == null) {
            return null;
          }
          // A super class is providing that type argument
          Type.ClassType ownerType = (Type.ClassType) nodeSymbol.owner.type;
          Set<AnnotatedTypeMirror.AnnotatedDeclaredType> superTypes =
              AnnotatedTypes.getSuperTypes(receiverDeclaredType);
          AnnotatedTypeMirror.AnnotatedDeclaredType superTypeMirror =
              superTypes.stream()
                  .filter(t -> ((Type.ClassType) t.getUnderlyingType()).tsym.equals(ownerType.tsym))
                  .findFirst()
                  .orElse(null);
          if (superTypeMirror != null) {
            superTypeMirror = superTypeMirror.deepCopy(true);
            Type.ClassType superTypeClassType =
                (Type.ClassType) superTypeMirror.getUnderlyingType();
            // Found the super type providing that type argument
            List<String> tvnames =
                TypeUtils.getTypeVariables(superTypeClassType).stream()
                    .map(t -> t.tsym.name.toString())
                    .collect(Collectors.toList());
            List<AnnotatedTypeMirror> ata = new ArrayList<>(superTypeMirror.getTypeArguments());
            int ii = tvnames.indexOf(typeVarName);
            AnnotatedTypeMirror typeArgumentType = ata.get(ii);
            typeFactory.makeUntainted(typeArgumentType, lists);
            return FoundRequired.of(receiverDeclaredType, superTypeMirror, pair.depth);
          }
        }
        if (i == -1) {
          continue;
        }
        AnnotatedTypeMirror typeArgumentType = allTypeArguments.get(i);
        typeFactory.makeUntainted(typeArgumentType, lists);
      }
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

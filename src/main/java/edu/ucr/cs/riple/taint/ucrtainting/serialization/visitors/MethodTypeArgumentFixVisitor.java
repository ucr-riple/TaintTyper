package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Visitor used to match the required type argument using method type arguments and passed
 * parameters parameter types.
 */
public class MethodTypeArgumentFixVisitor extends SpecializedFixComputer {

  private final Types types;

  public MethodTypeArgumentFixVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer) {
    super(context, factory, fixComputer);
    this.types = Types.instance(context);
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Set<Fix> fixes = new HashSet<>();
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    Set<Type.TypeVar> effectiveTypes = checkMethodTypeVarImpact(calledMethod, pair);
    if (!effectiveTypes.isEmpty()) {
      for (Type.TypeVar typeVar : effectiveTypes) {
        AnnotatedTypeFactory.ParameterizedExecutableType mType = typeFactory.methodFromUse(node);
        AnnotatedTypeMirror.AnnotatedExecutableType invokedMethod = mType.executableType;
        List<AnnotatedTypeMirror> paramsAnnotatedTypeMirrors =
            AnnotatedTypes.adaptParameters(typeFactory, invokedMethod, node.getArguments());
        for (int i = 0; i < node.getArguments().size(); i++) {
          AnnotatedTypeMirror requiredParam = paramsAnnotatedTypeMirrors.get(i).deepCopy(true);
          Type paramType = calledMethod.getParameters().get(i).type;
          boolean changed = updateAnnotatedTypeMirror(requiredParam, paramType, typeVar);
          if (changed) {
            ExpressionTree arg = node.getArguments().get(i);
            AnnotatedTypeMirror paramAnnotatedType = typeFactory.getAnnotatedType(arg);
            FoundRequired newPair =
                new FoundRequired(paramAnnotatedType, requiredParam, pair.depth);
            TypeMirror paramTypeMirror = paramAnnotatedType.getUnderlyingType();
            if ((paramTypeMirror instanceof Type.ClassType)
                && (requiredParam.getUnderlyingType() instanceof Type.ClassType)) {
              Type.ClassType nodeClassType = (Type.ClassType) paramTypeMirror;
              Type.ClassType requiredClassType = (Type.ClassType) requiredParam.getUnderlyingType();
              // check we are type matching a raw type on a class with type.
              if (nodeClassType.tsym.type.getTypeArguments().isEmpty()
                  && !requiredClassType.tsym.type.getTypeArguments().isEmpty()) {
                Set<Fix> onDeclaration =
                    computeFixesOnClassDeclarationForRawType(arg, newPair, typeVar);
                if (!onDeclaration.isEmpty()) {
                  return onDeclaration;
                }
              }
            }
            fixes.addAll(
                node.getArguments().get(i).accept(new FixComputer(context, typeFactory), newPair));
          }
        }
      }
    }
    return fixes;
  }

  /**
   * Checks if making the given type variable impact the given found type to be closer to the
   * required type.
   *
   * @param calledMethod The method to check.
   * @param pair The found and required types.
   * @return The set of type variables that can impact the found type to be closer to the required
   *     type.
   */
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
    if (type instanceof Type.ArrayType) {
      AnnotatedTypeMirror.AnnotatedArrayType foundArray =
          (AnnotatedTypeMirror.AnnotatedArrayType) found;
      AnnotatedTypeMirror.AnnotatedArrayType requiredArray =
          (AnnotatedTypeMirror.AnnotatedArrayType) required;
      Type.ArrayType arrayType = (Type.ArrayType) type;
      return typeVarCanImpactFoundRequiredPair(
          var,
          arrayType.getComponentType(),
          foundArray.getComponentType(),
          requiredArray.getComponentType());
    }
    return false;
  }

  /**
   * Updates the given annotated type mirror by replacing the annotation on the given type variable
   * to be @Runtainted.
   *
   * @param typeMirror The type mirror to update.
   * @param elementType The type to check.
   * @param var The type variable to check.
   * @return True if the type mirror was updated.
   */
  private boolean updateAnnotatedTypeMirror(
      AnnotatedTypeMirror typeMirror, Type elementType, Type.TypeVar var) {
    boolean updated = false;
    if (elementType instanceof Type.TypeVar && elementType.equals(var)) {
      typeFactory.makeUntainted(typeMirror);
      return true;
    }
    if (elementType instanceof Type.ClassType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) typeMirror;
      Type.ClassType classType = (Type.ClassType) elementType;
      for (int i = 0; i < classType.getTypeArguments().size(); i++) {
        AnnotatedTypeMirror paramTypeMirror = declaredType.getTypeArguments().get(i);
        Type paramType = classType.getTypeArguments().get(i);
        updated = updated || updateAnnotatedTypeMirror(paramTypeMirror, paramType, var);
      }
    }
    if (elementType instanceof Type.WildcardType) {
      AnnotatedTypeMirror.AnnotatedWildcardType wildcardType =
          (AnnotatedTypeMirror.AnnotatedWildcardType) typeMirror;
      Type.WildcardType wildcard = (Type.WildcardType) elementType;
      AnnotatedTypeMirror extendsBound = wildcardType.getExtendsBound();
      if (extendsBound != null) {
        updated = updateAnnotatedTypeMirror(extendsBound, wildcard.getExtendsBound(), var);
      }
    }
    if (elementType instanceof Type.ArrayType) {
      AnnotatedTypeMirror.AnnotatedArrayType arrayType =
          (AnnotatedTypeMirror.AnnotatedArrayType) typeMirror;
      Type.ArrayType array = (Type.ArrayType) elementType;
      updated =
          updateAnnotatedTypeMirror(arrayType.getComponentType(), array.getComponentType(), var);
    }
    return updated;
  }

  public Set<Fix> computeFixesOnClassDeclarationForRawType(
      Tree node, FoundRequired pair, Type.TypeVar typeVar) {
    Type type = Utility.getType(TreeUtils.elementFromTree(node));
    if (!(type.tsym instanceof Symbol.ClassSymbol)) {
      return Set.of();
    }
    Symbol.ClassSymbol classType = (Symbol.ClassSymbol) type.tsym;
    if (!typeFactory.isAnnotatedPackage(type.tsym.packge().fullname.toString())
        || !(pair.required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return Set.of();
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType required =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) pair.required;
    Type.ClassType requiredType =
        (Type.ClassType) ((Type.ClassType) required.getUnderlyingType()).tsym.type;
    Type.ClassType inheritedType = locateInheritedTypeOnExtendOrImplement(classType, requiredType);
    if(inheritedType == null){
      return Set.of();
    }

    // We intentionally limit the search to only the first level of inheritance. The type must
    // either extend or implement the required type explicitly at the declaration.
    throw new RuntimeException("Not implemented");
    //        return Set.of();
  }

  private Type.ClassType locateInheritedTypeOnExtendOrImplement(Symbol.ClassSymbol classType, Type.ClassType requiredType) {
    // Look for interfaces
    for (Type type : ((Type.ClassType) classType.type).interfaces_field) {
      if(type.tsym.equals(requiredType.tsym)){
        return (Type.ClassType) type;
      }
    }
    return null;
  }
}

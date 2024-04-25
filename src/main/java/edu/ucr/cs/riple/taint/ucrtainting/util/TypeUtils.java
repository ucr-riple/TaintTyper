package edu.ucr.cs.riple.taint.ucrtainting.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class TypeUtils {

  private TypeUtils() {
    // This class is only a utility class and should not be instantiated
  }

  /**
   * Gets the type of the given element. If the given element is a method, then the return type of
   * the method is returned.
   *
   * @param element The element to get the type for.
   * @return The type of the given element.
   */
  public static Type getType(Element element) {
    if (SymbolUtils.isConstructor(element)) {
      return ((Symbol.MethodSymbol) element).enclClass().type;
    }
    return element instanceof Symbol.MethodSymbol
        ? ((Symbol.MethodSymbol) element).getReturnType()
        : ((Symbol) element).type;
  }

  /**
   * Gets the type of the given expression tree.
   *
   * @param tree The element to get the type for.
   * @return The type of the given tree.
   */
  public static Type getType(ExpressionTree tree) {
    if (tree instanceof MethodInvocationTree) {
      return ((JCTree.JCMethodInvocation) tree).type;
    }
    if (tree instanceof MemberReferenceTree) {
      return ((JCTree.JCMemberReference) tree).type;
    }
    if (tree instanceof IdentifierTree) {
      return ((JCTree.JCIdent) tree).type;
    }
    if (tree instanceof NewClassTree) {
      return ((JCTree.JCNewClass) tree).type;
    }
    if (tree instanceof JCTree.JCFieldAccess) {
      return ((JCTree.JCFieldAccess) tree).type;
    }
    throw new IllegalArgumentException("Unsupported tree type: " + tree.getKind());
  }

  /**
   * Checks if the given element has a raw type. (e.g. {@code Foo} instead of {@code Foo<String>})
   *
   * @param element The element to check
   * @return true if the element has a raw type, false otherwise
   */
  public static boolean elementHasRawType(Element element) {
    return getType(element).isRaw();
  }

  /**
   * Checks if a type contains a typ argument. (e.g. {@code Foo<E>})
   *
   * @param type the type to check
   * @return true if the type contains a typ argument, false otherwise
   */
  public static boolean containsTypeArgument(Type type) {
    if (type instanceof Type.TypeVar) {
      return true;
    }
    if (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      for (Type t : classType.allparams()) {
        if (containsTypeArgument(t)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean containsTypeArgument(Type type, Type.TypeVar var) {
    if (type instanceof Type.TypeVar) {
      return type.equals(var);
    }
    if (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      for (Type t : classType.allparams()) {
        if (containsTypeArgument(t, var)) {
          return true;
        }
      }
    }
    if (type instanceof Type.ArrayType) {
      return containsTypeArgument(((Type.ArrayType) type).getComponentType(), var);
    }
    return false;
  }

  /**
   * Returns the list of all (including nested in top level) type variables of the for the given
   * element's type (e.g. {@code List<String> will return E and for Bar<R>.Foo<E> will return [R,
   * E]}).
   *
   * @param elementType The element to get the type arguments for.
   * @return The list of type arguments for the given element.
   */
  public static List<Type.TypeVar> getTypeVariables(Type elementType) {
    List<Type> typeArgsList =
        elementType instanceof Type.ClassType
            // Should return all type arguments, including those of the outer class.
            ? elementType.tsym.type.allparams()
            : elementType.tsym.type.getTypeArguments();
    // Should return as list to preserve the order of the type variables.
    return typeArgsList.stream().map(type -> (Type.TypeVar) type).collect(Collectors.toList());
  }

  /**
   * Gets the annotated type mirror of the containing type parameter of the given element.
   *
   * @param type the type to get the containing type parameter.
   * @param position the position of the type parameter.
   * @return the annotated type mirror of the containing type parameter.
   */
  public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfTypeArgumentAt(
      AnnotatedTypeMirror type, TypeIndex position) {
    TypeIndex copy = position.copy();
    if (copy.isEmpty()) {
      return type;
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedExecutableType) {
      AnnotatedTypeMirror.AnnotatedExecutableType declaredType =
          (AnnotatedTypeMirror.AnnotatedExecutableType) type;
      return getAnnotatedTypeMirrorOfTypeArgumentAt(declaredType.getReturnType(), copy);
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      return getAnnotatedTypeMirrorOfTypeArgumentAt(
          ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType(), copy);
    }
    int index = copy.poll();
    if (index == 0) {
      return type;
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
      return getAnnotatedTypeMirrorOfTypeArgumentAt(
          declaredType.getTypeArguments().get(index - 1), copy);
    }
    throw new IllegalArgumentException("Type " + type + " does not have type arguments.");
  }

  /**
   * Checks if the given type is a parameterized type. (e.g. {@code Foo<String>}, please note:
   * {@code Bar<E>.Foo<String>} is not a fully parameterized type)
   *
   * @param type the type to check
   * @return true if the type is a parameterized type, false otherwise
   */
  public static boolean isFullyParameterizedType(Type type) {
    if (!(type instanceof Type.ClassType)) {
      return false;
    }
    Type.ClassType classType = (Type.ClassType) type;
    if (classType.getTypeArguments().isEmpty()) {
      return false;
    }
    return !containsTypeArgument(type);
  }

  /**
   * Checks if the passed type has an untainted annotation.
   *
   * @param type the type to check.
   * @return true if the type has an untainted annotation, false otherwise.
   */
  public static boolean hasUntaintedAnnotation(Type type) {
    if (type == null) {
      return false;
    }
    if (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      for (AnnotationMirror annotation : classType.getAnnotationMirrors()) {
        if (annotation
            .getAnnotationType()
            .asElement()
            .getSimpleName()
            .contentEquals("RUntainted")) {
          return true;
        }
      }
    }
    return false;
  }
}

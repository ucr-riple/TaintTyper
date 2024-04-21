package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.base.Preconditions;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Utility methods for the serialization service. */
public class Utility {

  private Utility() {
    // This class is mainly a collection of static methods and should not be instantiated.
  }

  /**
   * Locates the declaration tree for a given symbol.
   *
   * @param sym the symbol to locate.
   * @param context the javac context.
   * @return the declaration tree or null if the declaration cannot be found.
   */
  @Nullable
  public static JCTree locateDeclaration(Symbol sym, Context context) {
    return (JCTree) Trees.instance(JavacProcessingEnvironment.instance(context)).getTree(sym);
  }

  /**
   * find the closest ancestor method in a superclass or superinterface that method overrides
   *
   * @param method the subclass method
   * @param types the types data structure from javac
   * @return closest overridden ancestor method, or <code>null</code> if method does not override
   *     anything
   */
  public static Symbol.MethodSymbol getClosestOverriddenMethod(
      Symbol.MethodSymbol method, Types types) {
    List<Symbol.MethodSymbol> ans = new ArrayList<>();
    // taken from Error Prone MethodOverrides check
    Symbol.ClassSymbol owner = method.enclClass();
    for (Type s : types.closure(owner.type)) {
      if (types.isSameType(s, owner.type)) {
        continue;
      }
      for (Symbol m : s.tsym.members().getSymbolsByName(method.name)) {
        if (!(m instanceof Symbol.MethodSymbol)) {
          continue;
        }
        Symbol.MethodSymbol memberSymbol = (Symbol.MethodSymbol) m;
        if (memberSymbol.isStatic()) {
          continue;
        }
        if (method.overrides(memberSymbol, owner, types, /*checkReturn*/ false)) {
          ans.add(memberSymbol);
          if (ans.size() > 1) {
            break;
          }
        }
      }
    }
    // in case one method is inherited by an interface and a super class, we have to choose one that
    // is inside source.
    if (ans.isEmpty()) {
      return null;
    }
    if (ans.size() == 1 || ans.get(0).enclClass().sourcefile != null) {
      return ans.get(0);
    }
    if (ans.get(1).enclClass().sourcefile != null) {
      return ans.get(1);
    }
    return ans.get(0);
  }

  /**
   * Checks if the given element is a constructor.
   *
   * @param element The element to check.
   * @return true if the given element is a constructor, false otherwise.
   */
  public static boolean isConstructor(Element element) {
    if (!(element instanceof Symbol.MethodSymbol)) {
      return false;
    }
    return ((Symbol.MethodSymbol) element).name.toString().equals("<init>");
  }

  /**
   * Gets the type of the given element. If the given element is a method, then the return type of
   * the method is returned.
   *
   * @param element The element to get the type for.
   * @return The type of the given element.
   */
  public static Type getType(Element element) {
    if (isConstructor(element)) {
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
   * Given a TreePath, finds the first enclosing node of the given type and returns the path from
   * the enclosing node to the top-level {@code CompilationUnitTree}.
   */
  public static <T> TreePath findPathFromEnclosingNodeToTopLevel(TreePath path, Class<T> klass) {
    if (path != null) {
      do {
        path = path.getParentPath();
      } while (path != null && !klass.isInstance(path.getLeaf()));
    }
    return path;
  }

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type. Returns null if no
   * such node is found.
   */
  @Nullable
  public static <T> T findEnclosingNode(TreePath path, Class<T> klass) {
    path = findPathFromEnclosingNodeToTopLevel(path, klass);
    return (path == null) ? null : klass.cast(path.getLeaf());
  }

  @Nullable
  public static Symbol.ClassSymbol findRegionClassSymbol(TreePath path) {
    // If path is on a class, that class itself is the region class. Otherwise, use the enclosing
    // class.
    ClassTree classTree =
        path.getLeaf() instanceof ClassTree
            ? (ClassTree) path.getLeaf()
            : findEnclosingNode(path, ClassTree.class);
    return classTree != null
        ? (Symbol.ClassSymbol) TreeUtils.elementFromDeclaration(classTree)
        : null;
  }

  @Nullable
  public static Symbol findRegionMemberSymbol(
      @Nullable Symbol.ClassSymbol regionClass, TreePath path) {
    if (regionClass == null) {
      return null;
    }
    Symbol ans = null;
    MethodTree enclosingMethod;
    // If the error is reported on a method, that method itself is the relevant program point.
    // Otherwise, use the enclosing method (if present).
    enclosingMethod =
        path.getLeaf() instanceof MethodTree
            ? (MethodTree) path.getLeaf()
            : findEnclosingNode(path, MethodTree.class);
    if (enclosingMethod != null) {
      // It is possible that the computed method is not enclosed by the computed class, e.g., for
      // the following case:
      //  class C {
      //    void foo() {
      //      class Local {
      //        Object f = null; // error
      //      }
      //    }
      //  }
      // Here the above code will compute clazz to be Local and method as foo().  In such cases,
      // set method to null, we always want the corresponding method to be nested in the
      // corresponding class.
      Symbol.MethodSymbol methodSymbol =
          (Symbol.MethodSymbol) TreeUtils.elementFromDeclaration(enclosingMethod);
      if (methodSymbol != null && !methodSymbol.isEnclosedBy(regionClass)) {
        enclosingMethod = null;
      }
    }
    if (enclosingMethod != null) {
      ans = (Symbol) TreeUtils.elementFromDeclaration(enclosingMethod);
    } else {
      // Node is not enclosed by any method, can be a field declaration or enclosed by it.
      Symbol sym = (Symbol) TreeUtils.elementFromTree(path.getLeaf());
      Symbol.VarSymbol fieldSymbol = null;
      if (sym != null && sym.getKind().isField() && sym.isEnclosedBy(regionClass)) {
        // Directly on a field declaration.
        fieldSymbol = (Symbol.VarSymbol) sym;
      } else {
        // Can be enclosed by a field declaration tree.
        VariableTree fieldDeclTree = findEnclosingNode(path, VariableTree.class);
        if (fieldDeclTree != null) {
          fieldSymbol = (Symbol.VarSymbol) TreeUtils.elementFromDeclaration(fieldDeclTree);
        }
      }
      if (fieldSymbol != null && fieldSymbol.isEnclosedBy(regionClass)) {
        ans = fieldSymbol;
      }
    }
    return ans;
  }

  /**
   * Returns true if the passed tree is the {@code this} identifier.
   *
   * @param tree Tree to check.
   * @return true, it the tree is {@code this} identifier.
   */
  public static boolean isThisIdentifier(Tree tree) {
    return tree instanceof IdentifierTree
        && ((IdentifierTree) tree).getName().contentEquals("this");
  }

  /**
   * Returns true if the passed tree is the {@code super} identifier.
   *
   * @param tree Tree to check.
   * @return true, it the tree is {@code super} identifier.
   */
  public static boolean isSuperIdentifier(Tree tree) {
    return tree instanceof IdentifierTree
        && ((IdentifierTree) tree).getName().contentEquals("super");
  }

  /**
   * Checks if the passed element is an enum constant.
   *
   * @param element the element to check.
   * @return true if the element is an enum constant, false otherwise.
   */
  public static boolean isEnumConstant(Element element) {
    if (element instanceof Symbol.VarSymbol) {
      return ((Symbol.VarSymbol) element).isEnum();
    }
    return false;
  }

  /**
   * Returns true if the passed tree is a literal or primitive.
   *
   * @param tree the tree to check.
   * @return true if the tree is a literal or primitive.
   */
  public static boolean isLiteralOrPrimitive(Tree tree) {
    if (tree == null) {
      return false;
    }
    switch (tree.getKind()) {
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case CHAR_LITERAL:
      case BOOLEAN_LITERAL:
      case NULL_LITERAL:
      case STRING_LITERAL:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if the passed element is a static and final field.
   *
   * @param element the element to check.
   * @return true if the element is a static and final field, false otherwise.
   */
  public static boolean isStaticAndFinalField(Element element) {
    if (element == null || !element.getKind().isField()) {
      return false;
    }
    if (element instanceof Symbol.VarSymbol) {
      Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
      if (varSymbol.getKind() == ElementKind.FIELD) {
        Symbol.ClassSymbol encClass = varSymbol.enclClass();
        if (encClass != null && encClass.isInterface()) {
          // All fields in interfaces are implicitly static and final.
          return true;
        }
      }
    }
    return ElementUtils.isFinal(element) && ElementUtils.isStatic(element);
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

  /**
   * Checks if the passed treePath is invoked as part of an if condition
   *
   * @param treePath the treePath to check.
   * @return true if the passed treePath is invoked as part of an if condition
   */
  public static boolean isMethodInvocationInIfConditional(TreePath treePath) {
    TreePath parent = treePath.getParentPath();
    while (parent != null
        && parent.getLeaf().getKind() != Tree.Kind.BLOCK
        && parent.getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      if (parent.getLeaf().getKind() == Tree.Kind.IF) {
        return true;
      }
      parent = parent.getParentPath();
    }
    return false;
  }

  /**
   * Returns the arguments of a callable tree. A callable tree is either a method invocation or a
   * new class.
   *
   * @param tree the tree to get the arguments from.
   * @return the arguments of the callable tree.
   */
  public static List<? extends ExpressionTree> getCallableArguments(Tree tree) {
    if (tree instanceof MethodInvocationTree) {
      return ((MethodInvocationTree) tree).getArguments();
    }
    if (tree instanceof NewClassTree) {
      return ((NewClassTree) tree).getArguments();
    }
    return null;
  }

  /**
   * Returns the arguments of a callable tree. A callable tree is either a method invocation or a
   * new class.
   *
   * @param tree the tree to get the arguments from.
   * @return the arguments of the callable tree.
   */
  public static List<Symbol.VarSymbol> getCallableArgumentsSymbol(Tree tree) {
    Element element = TreeUtils.elementFromTree(tree);
    if (element instanceof Symbol.MethodSymbol) {
      return ((Symbol.MethodSymbol) element).params();
    }
    return null;
  }

  /**
   * Returns the path to the source file of the given symbol.
   *
   * @param symbol the symbol to get the source file for.
   * @return the path to the source file of the given symbol.
   */
  public static Path getPathFromSymbol(Symbol symbol) {
    Symbol.ClassSymbol enclosingClass =
        symbol instanceof Symbol.ClassSymbol ? (Symbol.ClassSymbol) symbol : symbol.enclClass();
    URI pathInURI =
        enclosingClass.sourcefile != null
            ? enclosingClass.sourcefile.toUri()
            : (enclosingClass.classfile != null ? enclosingClass.classfile.toUri() : null);
    return Serializer.pathToSourceFileFromURI(pathInURI);
  }

  public static Symbol.MethodSymbol getFunctionalInterfaceMethod(ExpressionTree tree, Types types) {
    Preconditions.checkArgument(
        (tree instanceof LambdaExpressionTree) || (tree instanceof MemberReferenceTree));
    Type funcInterfaceType = ((JCTree.JCFunctionalExpression) tree).type;
    return (Symbol.MethodSymbol) types.findDescriptorSymbol(funcInterfaceType.tsym);
  }
}

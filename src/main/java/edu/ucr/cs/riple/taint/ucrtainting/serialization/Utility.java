package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

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
    if (sym == null) {
      return null;
    }
    Env<AttrContext> enterEnv = Enter.instance(context).getEnv(sym.enclClass());
    if (enterEnv == null) {
      return null;
    }
    return TreeInfo.declarationFor(sym, enterEnv.tree);
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
          return memberSymbol;
        }
      }
    }
    return null;
  }

  /**
   * Gets the type of the given element. If the given element is a method, then the return type of
   * the method is returned.
   *
   * @param element The element to get the type for.
   * @return The type of the given element.
   */
  public static Type getType(Element element) {
    return element instanceof Symbol.MethodSymbol
        ? ((Symbol.MethodSymbol) element).getReturnType()
        : ((Symbol) element).type;
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

  /**
   * Gets the annotated type mirror of the containing type parameter of the given element.
   *
   * @param type the type to get the containing type parameter.
   * @param positions the positions of the type parameter.
   * @return the annotated type mirror of the containing type parameter.
   */
  public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfTypeArgumentAt(
      AnnotatedTypeMirror type, List<Integer> positions) {
    return getAnnotatedTypeMirrorOfTypeArgumentAt(type, new ArrayDeque<>(positions));
  }

  /**
   * Gets the annotated type mirror of the containing type parameter of the given element.
   *
   * @param type the type to get the containing type parameter.
   * @param position the position of the type parameter.
   * @return the annotated type mirror of the containing type parameter.
   */
  public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfTypeArgumentAt(
      AnnotatedTypeMirror type, Deque<Integer> position) {
    if (position.isEmpty()) {
      return type;
    }
    int index = position.poll();
    if (index == 0) {
      return type;
    }
    if (!(type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      throw new RuntimeException("Unexpected type: " + type);
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
    return getAnnotatedTypeMirrorOfTypeArgumentAt(
        declaredType.getTypeArguments().get(index - 1), position);
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
    if (classType.getTypeArguments().size() == 0) {
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
   * Checks if the passed symbol is in an annotated package.
   *
   * @param symbol the symbol to check.
   * @param typeFactory the type factory, used to retrieve the annotated packages names.
   * @return true if the symbol is in an annotated package, false otherwise.
   */
  public static boolean isInAnnotatedPackage(
      Symbol symbol, UCRTaintingAnnotatedTypeFactory typeFactory) {
    if (symbol == null) {
      return false;
    }
    Symbol.ClassSymbol encClass =
        symbol instanceof Symbol.ClassSymbol ? (Symbol.ClassSymbol) symbol : symbol.enclClass();
    String packageName = encClass.packge().toString();
    if (packageName.equals("unnamed package")) {
      packageName = "";
    }
    boolean fromAnnotatedPackage = typeFactory.isAnnotatedPackage(packageName);
    if (!fromAnnotatedPackage) {
      return false;
    }
    URI pathInURI =
        encClass.sourcefile != null
            ? encClass.sourcefile.toUri()
            : (encClass.classfile != null ? encClass.classfile.toUri() : null);
    return Serializer.pathToSourceFileFromURI(pathInURI) != null;
  }

  /**
   * Returns true if the passed tree is an enum constant.
   *
   * @param tree the tree to check.
   * @return true if the tree is an enum constant.
   */
  public static boolean isEnumConstant(Tree tree) {
    Element element = TreeUtils.elementFromTree(tree);
    if (element == null) {
      return false;
    }
    return isEnumConstant(element);
  }

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
    while(parent != null && parent.getLeaf().getKind() != Tree.Kind.BLOCK && parent.getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      if(parent.getLeaf().getKind() == Tree.Kind.IF) {
        return true;
      }
      parent = parent.getParentPath();
    }
    return false;
  }
}

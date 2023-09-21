package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class CollectionHandler extends AbstractHandler {

  /** Types instance used to detect if type is a subtype of java.util.Collection. */
  private final Types types;
  /** {@link java.util.Collection} interface name. */
  private static final String COLLECTIONS_INTERFACE = java.util.Collection.class.getName();
  /** {@link java.util.Collection#toArray()} method name. */
  private static final String TO_ARRAY_METHOD_NAME = "toArray";

  public CollectionHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    super(typeFactory);
    this.types = Types.instance(context);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (!(type instanceof AnnotatedTypeMirror.AnnotatedArrayType)) {
      return;
    }
    if (!isToArrayWithTypeArgMethod(calledMethod, types)) {
      return;
    }
    ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
    if (receiver == null || Utility.isThisIdentifier(receiver)) {
      return;
    }
    if (!(((JCTree) receiver).type instanceof Type.ClassType)) {
      throw new RuntimeException("CollectionHandler: receiver is not a class type");
    }
    AnnotatedTypeMirror receiverType = typeFactory.getReceiverType(tree);
    Type collectionType = getCollectionTypeFromType(receiverType);
    if (collectionType == null) {
      return;
    }
    if (((Type.ClassType) collectionType).typarams_field.isEmpty()) {
      return;
    }
    if (Utility.hasUntaintedAnnotation(((Type.ClassType) collectionType).typarams_field.get(0))) {
      typeFactory.makeUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
  }

  /**
   * Check if the type implements the {@link java.util.Collection} interface.
   *
   * @param type The type to check.
   * @return True if the type implements the {@link java.util.Collection} interface.
   */
  private static boolean implementsCollectionInterface(Type type, Types types) {
    if (type == null || type.tsym == null) {
      return false;
    }
    if (type.isInterface() && type.tsym.toString().equals(COLLECTIONS_INTERFACE)) {
      return true;
    }
    boolean implementsCollection =
        types.interfaces(type).stream()
            .anyMatch(intFace -> implementsCollectionInterface(intFace, types));
    if (implementsCollection) {
      return true;
    }
    return implementsCollectionInterface(types.supertype(type), types);
  }

  /**
   * Check if the type is an array with component type being the given type variable.
   *
   * @param type The type to check.
   * @param typeVar The type variable to check.
   * @return True if the type is an array with component type being the given type variable.
   */
  private static boolean isArrayTypeOfTypeArg(Type type, Symbol.TypeVariableSymbol typeVar) {
    return type instanceof Type.ArrayType && ((Type.ArrayType) type).elemtype.tsym.equals(typeVar);
  }

  /**
   * Check if the method is {@link java.util.Collection#toArray(Object[])} .
   *
   * @param symbol The method symbol to check.
   * @param types The types instance.
   * @return True if the method is {@link java.util.Collection#toArray(Object[])}.
   */
  public static boolean isToArrayWithTypeArgMethod(Symbol.MethodSymbol symbol, Types types) {
    // Check method name
    if (!symbol.getSimpleName().toString().equals(TO_ARRAY_METHOD_NAME)) {
      return false;
    }
    // Check if the method has a type argument.
    if (symbol.getTypeParameters().size() != 1) {
      return false;
    }
    // Check if the method has a single parameter.
    if (symbol.getParameters().size() != 1) {
      return false;
    }
    // Check if class is subclass of Collection
    if (!implementsCollectionInterface(symbol.enclClass().type, types)) {
      return false;
    }
    // Check parameter type.
    if (!isArrayTypeOfTypeArg(
        symbol.getParameters().get(0).type, symbol.getTypeParameters().get(0))) {
      return false;
    }
    // Check return type.
    return isArrayTypeOfTypeArg(symbol.getReturnType(), symbol.getTypeParameters().get(0));
  }

  /**
   * Retrieve the {@link java.util.Collection} type from the given type.
   *
   * @param type The type to retrieve the {@link java.util.Collection} type from.
   * @return The {@link java.util.Collection} type from the given type.
   */
  public static Type getCollectionTypeFromType(Type type) {
    Type collectionType = null;
    while (type instanceof Type.ClassType) {
      Type.ClassType classType = (Type.ClassType) type;
      if (classType.interfaces_field != null) {
        for (Type iFace : classType.interfaces_field) {
          if (iFace.tsym instanceof Symbol.ClassSymbol
              && ((Symbol.ClassSymbol) iFace.tsym)
                  .fullname
                  .toString()
                  .equals("java.util.Collection")) {
            collectionType = iFace;
          }
        }
        if (collectionType != null) {
          break;
        }
      }
      type = ((Type.ClassType) type).supertype_field;
    }
    return collectionType;
  }

  /**
   * Retrieve the {@link java.util.Collection} type from the given annotated type mirror.
   *
   * @param mirror The annotated type mirror to retrieve the {@link java.util.Collection} type from.
   * @return The {@link java.util.Collection} type from the given annotated type mirror.
   */
  public static Type getCollectionTypeFromType(AnnotatedTypeMirror mirror) {
    return getCollectionTypeFromType((Type) mirror.getUnderlyingType());
  }

  /**
   * Retrieve the symbolic {@link java.util.Collection} type from the given type.
   *
   * @param mirror The type to retrieve the symbolic {@link java.util.Collection} type from.
   * @return The symbolic {@link java.util.Collection} type from the given type.
   */
  public static Type getSymbolicCollectionTypeFromType(AnnotatedTypeMirror mirror) {
    return getCollectionTypeFromType(((Type) mirror.getUnderlyingType()).tsym.type);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class CollectionHandler extends AbstractHandler {

  private final Types types;
  private static final String COLLECTIONS_INTERFACE = java.util.Collection.class.getName();
  private static final String TO_ARRAY_METHOD_NAME = "toArray";

  public CollectionHandler(UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    super(typeFactory);
    this.types = Types.instance(context);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (isToArrayWithTypeArgument(calledMethod)) {
      ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
      if (Utility.isThisIdentifier(receiver)) {
        return;
      }
      System.out.println(calledMethod);
    }
  }

  /**
   * Checks if the method is {@link java.util.Collection#toArray(Object[])}.
   *
   * @param symbol The method symbol to check.
   * @return True if the method is {@link java.util.Collection#toArray(Object[])}.
   */
  private boolean isToArrayWithTypeArgument(Symbol.MethodSymbol symbol) {
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
    if (!implementsCollectionInterface(symbol.enclClass().type)) {
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
   * Check if the type implements the {@link java.util.Collection} interface.
   *
   * @param type The type to check.
   * @return True if the type implements the {@link java.util.Collection} interface.
   */
  private boolean implementsCollectionInterface(Type type) {
    if (type == null) {
      return false;
    }
    if (type.isInterface() && type.tsym.toString().equals(COLLECTIONS_INTERFACE)) {
      return true;
    }
    List<Type> interfaces = types.interfaces(type);
    boolean ans =
        interfaces.stream()
            .anyMatch(
                intFace ->
                    intFace.tsym.isInterface()
                        && intFace.tsym.toString().equals(COLLECTIONS_INTERFACE));
    if (ans) {
      return true;
    }
    for (Type anInterface : interfaces) {
      if (implementsCollectionInterface(anInterface)) {
        return true;
      }
    }
    return false;
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
}

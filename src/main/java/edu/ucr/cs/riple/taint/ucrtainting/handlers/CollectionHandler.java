package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class CollectionHandler extends AbstractHandler {

  public CollectionHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
    super.visitMethodInvocation(tree, type);
    Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (!(type instanceof AnnotatedTypeMirror.AnnotatedArrayType)) {
      return;
    }
    if (!isToArrayWithTypeArgMethod(symbol)) {
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
    if (Utility.hasUntaintedAnnotation(((Type.ClassType) collectionType).typarams_field.get(0))) {
      typeFactory.makeUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    }
  }

  public static void overridesCollectionInterface() {
    // todo: merge from branch
  }

  public static boolean isToArrayWithTypeArgMethod(Symbol.MethodSymbol symbol) {
    // todo: merge from branch
    return symbol.name.toString().equals("toArray") && symbol.params().size() == 1;
  }

  private static Type getCollectionTypeFromType(Type type) {
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

  public static Type getCollectionTypeFromType(AnnotatedTypeMirror mirror) {
    return getCollectionTypeFromType((Type) mirror.getUnderlyingType());
  }

  public static Type getSymbolicCollectionTypeFromType(AnnotatedTypeMirror mirror) {
    return getCollectionTypeFromType(((Type) mirror.getUnderlyingType()).tsym.type);
  }
}

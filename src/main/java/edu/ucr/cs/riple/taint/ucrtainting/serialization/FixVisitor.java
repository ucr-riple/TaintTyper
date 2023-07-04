package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import static edu.ucr.cs.riple.taint.ucrtainting.Log.print;

import com.google.common.base.Preconditions;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** Generates the fixes for the given tree involved in the reporting error if such fixes exists. */
public class FixVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  private final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  private final UCRTaintingAnnotatedTypeFactory typeFactory;
  /** The tree that caused the error. */
  private final Tree errorTree;
  /**
   * The list method invocations that their return type contained a type argument. Used to detect
   * which type in the receiver should be annotated.
   */
  private List<ExpressionTree> receivers;
  /** Required annotated type in the assignment on the left hand side. */
  private final AnnotatedTypeMirror required;
  /** Found annotated type in the assignment on the right hand side. */
  private final AnnotatedTypeMirror found;

  boolean fixOnReceiver = false;
  boolean fixOnSymbol = true;

  public FixVisitor(
      Context context,
      UCRTaintingAnnotatedTypeFactory factory,
      Tree errorTree,
      AnnotatedTypeMirror required,
      AnnotatedTypeMirror found) {
    this.context = context;
    this.typeFactory = factory;
    this.errorTree = errorTree;
    this.required = required;
    this.found = found;
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, Void unused) {
    if (typeFactory.mayBeTainted(node)) {
      Fix fix = buildFixForElement(TreeUtils.elementFromTree(node));
      return fix == null ? Set.of() : Set.of(fix);
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getTrueExpression())) {
      fixes.addAll(
          new FixVisitor(context, typeFactory, node.getTrueExpression(), required, found)
              .generateFixes());
    }
    if (typeFactory.mayBeTainted(node.getFalseExpression())) {
      fixes.addAll(
          new FixVisitor(context, typeFactory, node.getFalseExpression(), required, found)
              .generateFixes());
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewClass(com.sun.source.tree.NewClassTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    for (ExpressionTree arg : node.getArguments()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(this, unused));
      }
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitTypeCast(TypeCastTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    if (typeFactory.mayBeTainted(node.getExpression())) {
      fixes.addAll(node.getExpression().accept(this, unused));
    }
    return fixes;
  }

  @Override
  public Set<Fix> visitNewArray(NewArrayTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    // Add a fix for each argument.
    if (node.getInitializers() != null) {
      for (ExpressionTree arg : node.getInitializers()) {
        if (typeFactory.mayBeTainted(arg)) {
          fixes.addAll(arg.accept(this, unused));
        }
      }
    }
    // Add a fix for each dimension.
    for (ExpressionTree arg : node.getDimensions()) {
      if (typeFactory.mayBeTainted(arg)) {
        fixes.addAll(arg.accept(this, unused));
      }
    }
    return fixes;
  }

  /**
   * Visitor for method invocations. For method invocations:
   *
   * <ol>
   *   <li>If return type is not type variable, we annotate the called method.
   *   <li>If return type is type variable and defined in source code, we annotate the called
   *       method.
   *   <li>If return type is type variable and defined in library, we annotate the receiver.
   * </ol>
   *
   * @param node The given tree.
   * @return Void null.
   */
  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, Void unused) {
    Element element = TreeUtils.elementFromUse(node);
    if (element == null) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) element;
    // check if the call is to a method defined in a third party library.
    if (!Utility.isInAnnotatedPackage(calledMethod, typeFactory)) {
      // Check if the method is source defined in stubs.
      if (typeFactory.isFromStubFile(calledMethod)) {
        // We cannot do any fix here
        return Set.of();
      }
      // Check the return type, if it does not contain type variable, we can annotate the
      // receiver and all passed arguments.
      if (!(Utility.containsTypeArgument(calledMethod.getReturnType()))) {
        Set<Fix> fixes = new HashSet<>();
        // Add a fix for each passed argument.
        for (ExpressionTree argument : node.getArguments()) {
          fixes.addAll(argument.accept(this, unused));
        }
        // Add the fix for the receiver if not static.
        if (calledMethod.isStatic()) {
          // No receiver for static method calls.
          return fixes;
        }
        // Build the fix for the receiver.
        fixes.addAll(
            ((MemberSelectTree) node.getMethodSelect()).getExpression().accept(this, unused));
        return fixes;
      }
    }
    // Locate method receiver.
    ExpressionTree receiver = null;
    if (node.getMethodSelect() instanceof MemberSelectTree) {
      receiver = ((MemberSelectTree) node.getMethodSelect()).getExpression();
    }
    // If method is static, or has no receiver, or receiver is "this", we must annotate the method
    // directly.
    if (calledMethod.isStatic() || receiver == null || Utility.isThisIdentifier(receiver)) {
      fixOnReceiver = false;
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
    }
    if (fixOnReceiver) {
      addReceiver(node);
    }
    // The method has a receiver, if the method contains a type argument, we should annotate the
    // receiver and leave the called method untouched. Annotation on the declaration on the type
    // argument, will be added on the method automatically.
    if (Utility.containsTypeArgument(calledMethod.getReturnType()) && !calledMethod.isStatic()) {
      if (!fixOnReceiver) {
        addReceiver(node);
      }
      return receiver.accept(this, unused);
    } else {
      fixOnReceiver = false;
      // Build a fix for the called method return type.
      return Set.of(Objects.requireNonNull(buildFixForElement(calledMethod)));
    }
  }

  @Override
  public Set<Fix> visitLiteral(LiteralTree node, Void unused) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
    // We do not generate fix for primitive types.
    return Set.of();
  }

  @Override
  public Set<Fix> visitExpressionStatement(ExpressionStatementTree node, Void unused) {
    return node.getExpression().accept(this, unused);
  }

  @Override
  public Set<Fix> visitBinary(BinaryTree node, Void unused) {
    Set<Fix> fixes = new HashSet<>();
    fixes.addAll(
        new FixVisitor(context, typeFactory, node.getLeftOperand(), required, found)
            .generateFixes());
    fixes.addAll(
        new FixVisitor(context, typeFactory, node.getRightOperand(), required, found)
            .generateFixes());
    return fixes;
  }

  public Set<Fix> visitArrayAccess(ArrayAccessTree node, Void unused) {
    // only the expression is enough, we do not need to annotate the index.
    return node.getExpression().accept(this, unused);
  }

  @Override
  public Set<Fix> visitMemberSelect(MemberSelectTree node, Void unused) {
    if (typeFactory.mayBeTainted(node.getExpression())) {
      Element member = TreeUtils.elementFromUse(node);
      if (!(member instanceof Symbol)) {
        return Set.of();
      }
      // If fix on receiver, we should annotate type parameter that matches the target type.
      if (fixOnReceiver) {
        // If is a parameterized type, then we found the right declaration.
        if (Utility.isParameterizedType(((Symbol) member).type)) {
          Fix fix = buildFixForElement(TreeUtils.elementFromUse(node));
          return fix == null ? Set.of() : Set.of(fix);
        } else if (node instanceof JCTree.JCFieldAccess) {
          // Need to traverse the tree to find the right declaration.
          JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
          addReceiver(fieldAccess);
          return fieldAccess.selected.accept(this, unused);
        }
      } else {
        Fix fix = buildFixForElement(TreeUtils.elementFromUse(node));
        return fix == null ? Set.of() : Set.of(fix);
      }
    }
    return Set.of();
  }

  @Override
  public Set<Fix> visitParenthesized(ParenthesizedTree node, Void unused) {
    return node.getExpression().accept(this, unused);
  }

  @Override
  public Set<Fix> visitUnary(UnaryTree node, Void unused) {
    return node.getExpression().accept(this, unused);
  }

  /**
   * Generates fixes for the given error tree.
   *
   * @return The set of fixes for the given error tree.
   */
  public Set<Fix> generateFixes() {
    return visit(errorTree, null);
  }

  private void setStateOnFixOnReceiver() {
    this.fixOnReceiver = true;
    this.fixOnSymbol = false;
  }

  private void setStateOnFixOnSymbol() {
    if (fixOnReceiver) {
      throw new RuntimeException("State already set");
    }
    fixOnSymbol = true;
  }

  private void addReceiver(ExpressionTree tree) {
    setStateOnFixOnReceiver();
    if (this.receivers == null) {
      this.receivers = new ArrayList<>();
    }
    this.receivers.add(0, tree);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element) {
    SymbolLocation location;
    if (element == null) {
      return null;
    }
    location = SymbolLocation.createLocationFromSymbol((Symbol) element, context);
    List<Integer> indexes = new ArrayList<>();
    if (fixOnReceiver) {
      // location requires a type variable modification.
      Type elementType = ((Symbol) element).type;
      if (receivers.size() == 1
          && required
              .getUnderlyingType()
              .equals(TreeUtils.elementFromUse(receivers.get(0)).asType())) {
        print("Type match, we should annotate just the elements type");
      } else {
        print("Type mismatch");
        logInvocation(element, elementType);
        Preconditions.checkArgument(elementType instanceof Type.ClassType);

        // Indexes of the type variables to locate the type which needs to be modified.
        Map<Type.TypeVar, Type.TypeVar> typeVarMap = new HashMap<>();
        List<Type> elementTypeArgs = getAllTypeArguments(elementType);
        getAllTypeArguments(elementType)
            .forEach(
                type -> {
                  Preconditions.checkArgument(type instanceof Type.TypeVar);
                  typeVarMap.put((Type.TypeVar) type, (Type.TypeVar) type);
                });
        for (ExpressionTree receiver : receivers) {
          // Locate passed type arguments
          Symbol receiverSymbol = (Symbol) TreeUtils.elementFromUse(receiver);
          Type receiverType =
              receiverSymbol instanceof Symbol.MethodSymbol
                  ? ((Symbol.MethodSymbol) receiverSymbol).getReturnType()
                  : receiverSymbol.type;
          List<Type> providedTypeArgsForReceiver = receiverType.getTypeArguments();

          // Update translation:
          List<Type> typeArguments = getAllTypeArguments(receiverType);
          for (int i = 0; i < providedTypeArgsForReceiver.size(); i++) {
            Type.TypeVar provided = (Type.TypeVar) providedTypeArgsForReceiver.get(i);
            if (typeVarMap.containsKey(provided)) {
              Type.TypeVar value = typeVarMap.get(provided);
              typeVarMap.remove(provided);
              typeVarMap.put((Type.TypeVar) typeArguments.get(i), value);
            }
          }

          if (receiverType instanceof Type.TypeVar) {
            // We should refresh base.
            Type.TypeVar original = typeVarMap.get((Type.TypeVar) (receiverType));
            int i;
            for (i = 0; i < elementTypeArgs.size(); i++) {
              if (elementTypeArgs.get(i).equals(original)) {
                indexes.add(i);
                break;
              }
            }
            elementType = getProvidedTypeArguments(element).get(i);
            elementTypeArgs = getAllTypeArguments(elementType);
            typeVarMap.clear();
            getAllTypeArguments(elementType)
                .forEach(
                    type -> {
                      Preconditions.checkArgument(type instanceof Type.TypeVar);
                      typeVarMap.put((Type.TypeVar) type, (Type.TypeVar) type);
                    });
          }
        }
      }
    }
    if (location == null) {
      return null;
    }
    return new Fix("untainted", location);
  }

  private void logInvocation(Element element, Type elementType) {
    receivers.forEach(
        type -> {
          ExpressionTree receiver = type;
          Symbol receiverSymbol = (Symbol) TreeUtils.elementFromTree(type);
          Type receiverType =
              receiverSymbol instanceof Symbol.MethodSymbol
                  ? ((Symbol.MethodSymbol) receiverSymbol).getReturnType()
                  : receiverSymbol.type;
          System.out.println(
              "CALLED METHOD: "
                  + receiverSymbol
                  + " - RETURN TYPE: "
                  + receiver
                  + " - CLASS: "
                  + receiverSymbol.enclClass().type.tsym.type
                  + " - RETURN TYPE SYM: "
                  + receiverType
                  + " - TYPE SYMBOL: "
                  + receiverType.tsym.type);
        });
    System.out.println("ELEMENT: " + element);
    System.out.println("TYPE: " + elementType + " - SYMBOL: " + elementType.tsym.type);
    System.out.println("REQUIRED: " + required);
    System.out.println("FOUND: " + found);
  }

  private List<Type> getAllTypeArguments(Type elementType) {
    if (elementType instanceof Type.ClassType) {
      return ((Type.ClassType) (elementType).tsym.type).allparams_field;
    } else {
      return elementType.tsym.type.getTypeArguments();
    }
  }

  private List<Type> getProvidedTypeArguments(Element element) {
    Symbol symbol = (Symbol) element;
    if (symbol.type instanceof Type.ClassType) {
      return ((Type.ClassType) symbol.type).allparams_field;
    }
    return symbol.type.getTypeArguments();
  }
}

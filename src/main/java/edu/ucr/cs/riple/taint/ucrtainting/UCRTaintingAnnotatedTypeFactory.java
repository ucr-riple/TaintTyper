package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.*;

public class UCRTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * This option enables custom handling of third party code. By default, such handling is enabled.
   */
  public final boolean ENABLE_CUSTOM_CHECK;
  /** List of annotated packages. Classes in these packages are considered to be annotated. */
  private final List<String> ANNOTATED_PACKAGE_NAMES_LIST;
  /** AnnotationMirror for {@link RUntainted}. */
  public final AnnotationMirror RUNTAINTED;
  /** AnnotationMirror for {@link RTainted}. */
  public final AnnotationMirror RTAINTED;

  final Set<Symbol.VarSymbol> staticFinalFieldsWithInitializer;

  public UCRTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    ENABLE_CUSTOM_CHECK = checker.getBooleanOption(UCRTaintingChecker.ENABLE_CUSTOM_CHECKER, true);
    String givenAnnotatedPackages = checker.getOption(UCRTaintingChecker.ANNOTATED_PACKAGES);
    // make sure that annotated package names are always provided and issue error otherwise
    if (givenAnnotatedPackages == null) {
      if (checker.hasOption(UCRTaintingChecker.ANNOTATED_PACKAGES)) {
        throw new UserError(
            "The value for the argument -AannotatedPackages"
                + " is null. Please pass this argument in the checker config, refer checker manual");
      } else {
        throw new UserError(
            "Cannot find this argument -AannotatedPackages"
                + " Please pass this argument in the checker config, refer checker manual");
      }
    }
    /**
     * To respect existing annotations, leave them alone based on the provided annotated package
     * names through this option.
     */
    String annotatedPackagesFlagValue =
        givenAnnotatedPackages.equals("\"\"") ? "" : givenAnnotatedPackages;
    ANNOTATED_PACKAGE_NAMES_LIST = Arrays.asList(annotatedPackagesFlagValue.split(","));
    RUNTAINTED = AnnotationBuilder.fromClass(elements, RUntainted.class);
    RTAINTED = AnnotationBuilder.fromClass(elements, RTainted.class);
    this.staticFinalFieldsWithInitializer = new HashSet<>();
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new UCRTaintingTreeAnnotator(this, staticFinalFieldsWithInitializer));
  }

  /**
   * Checks if any of the arguments of the node has been annotated with {@link RTainted}
   *
   * @param node to check for
   * @return true if any argument is annotated with {@link RTainted}, false otherwise
   */
  private boolean hasTaintedArgument(ExpressionTree node) {
    List<? extends ExpressionTree> argumentsList = null;
    if (node instanceof MethodInvocationTree) {
      argumentsList = ((MethodInvocationTree) node).getArguments();
    } else if (node instanceof NewClassTree) {
      argumentsList = ((NewClassTree) node).getArguments();
    }
    if (node instanceof TypeCastTree) {
      argumentsList = Collections.singletonList(((TypeCastTree) node).getExpression());
    }
    if (argumentsList != null) {
      for (ExpressionTree eTree : argumentsList) {
        try {
          if (getAnnotatedType(eTree).hasAnnotation(RTAINTED)) {
            return true;
          }
        } catch (BugInCF bug) {
          // TODO:: take care of errors
        }
      }
    }
    return false;
  }

  /**
   * Checks if the receiver tree has been annotated with {@link RTainted}
   *
   * @param node to check for
   * @return true if annotated with {@link RTainted}, false otherwise
   */
  private boolean hasTaintedReceiver(ExpressionTree node) {
    if (node != null) {
      ExpressionTree receiverTree = TreeUtils.getReceiverTree(node);
      if (receiverTree != null) {
        Element element = TreeUtils.elementFromTree(node);
        if (element != null) {
          Set<Modifier> modifiers = element.getModifiers();
          if (modifiers != null
              && !modifiers.contains(Modifier.STATIC)
              && getAnnotatedType(receiverTree).hasAnnotation(RTAINTED)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if the package for the node is present in already annotated according to provided
   * option.
   *
   * @param node to check for
   * @return true if present, false otherwise
   */
  private boolean isInAnnotatedPackage(ExpressionTree node) {
    Symbol symbol = (Symbol) TreeUtils.elementFromTree(node);
    if (symbol == null) {
      return false;
    }
    return Utility.isInAnnotatedPackage(symbol, this);
  }

  /**
   * Checks if the tree is annotated in the stub files
   *
   * @param node to check for
   * @return true if annotated, false otherwise
   */
  public boolean isPresentInStub(ExpressionTree node) {
    if (node != null) {
      Element elem = TreeUtils.elementFromTree(node);
      if (elem != null && isFromStubFile(elem)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the package name matches any of the annotated packages
   *
   * @param packageName to check for
   * @return true if matches, false otherwise
   */
  public boolean isAnnotatedPackage(String packageName) {
    for (String annotatedPackageName : ANNOTATED_PACKAGE_NAMES_LIST) {
      if (packageName.startsWith(annotatedPackageName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given tree may be tainted.
   *
   * @param tree The given tree.
   * @return True if the given tree is tainted, false otherwise.
   */
  public boolean mayBeTainted(Tree tree) {
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    return mayBeTainted(type);
  }

  /**
   * Checks if the given type may be tainted
   *
   * @param type The given type
   * @return True if the given type may be tainted, false otherwise.
   */
  public boolean mayBeTainted(AnnotatedTypeMirror type) {
    // If type is null, we should be conservative and assume it may be tainted.
    if (type == null) {
      return true;
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      return mayBeTainted((AnnotatedTypeMirror.AnnotatedArrayType) type);
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      return mayBeTainted((AnnotatedTypeMirror.AnnotatedDeclaredType) type);
    }
    return !type.hasAnnotation(RUNTAINTED);
  }

  /**
   * Checks if the given declared type may be tainted
   *
   * @param type The given declared type
   * @return True if the given declared type may be tainted, false otherwise.
   */
  public boolean mayBeTainted(AnnotatedTypeMirror.AnnotatedDeclaredType type) {
    if (type == null) {
      return true;
    }
    for (AnnotatedTypeMirror typeVariable : type.getTypeArguments()) {
      return mayBeTainted(typeVariable);
    }
    return !hasUntaintedAnnotation(type);
  }

  /**
   * Checks if the given array type's component may be tainted
   *
   * @param type The given array type
   * @return True if the given array type's component may be tainted, false otherwise.
   */
  public boolean mayBeTainted(AnnotatedTypeMirror.AnnotatedArrayType type) {
    return type == null || !hasUntaintedAnnotation(type.getComponentType());
  }

  public boolean hasUntaintedAnnotation(AnnotatedTypeMirror type) {
    return type.hasAnnotation(RUNTAINTED);
  }

  @Override
  protected void addAnnotationsFromDefaultForType(
      @Nullable Element element, AnnotatedTypeMirror type) {
    if (element instanceof Symbol.VarSymbol && staticFinalFieldsWithInitializer.contains(element)) {
      type.replaceAnnotation(RUNTAINTED);
    } else {
      super.addAnnotationsFromDefaultForType(element, type);
    }
  }

  private class UCRTaintingTreeAnnotator extends TreeAnnotator {

    private final Set<Symbol.VarSymbol> staticFinalVars;

    /**
     * UCRTaintingTreeAnnotator
     *
     * @param atypeFactory the type factory
     */
    protected UCRTaintingTreeAnnotator(
        AnnotatedTypeFactory atypeFactory, Set<Symbol.VarSymbol> staticFinalVars) {
      super(atypeFactory);
      this.staticFinalVars = staticFinalVars;
    }

    /**
     * Visits all method invocations and updates {@link AnnotatedTypeMirror} according to the
     * argument and receiver annotations. If any of the arguments or the receiver is {@link
     * RTainted}, the {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
     *
     * @param node the node being visited
     * @param annotatedTypeMirror annotated return type of the method invocation
     */
    @Override
    public Void visitMethodInvocation(
        MethodInvocationTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      if (ENABLE_CUSTOM_CHECK) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        if (!isInAnnotatedPackage(node) && !isPresentInStub(node)) {
          if (!hasTaintedArgument(node) && !hasTaintedReceiver(node)) {
            Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
            Type type = calledMethod.getReturnType();
            if (calledMethod.isStatic() || !(type instanceof Type.TypeVar)) {
              annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
            }
          }
        }
      }
      return super.visitMethodInvocation(node, annotatedTypeMirror);
    }

    @Override
    public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      if (ENABLE_CUSTOM_CHECK) {
        if (Utility.isEnumConstant(node)) {
          ExpressionTree initializer = node.getInitializer();
          if (!hasTaintedArgument(initializer)) {
            getAnnotatedType(initializer).replaceAnnotation(RUNTAINTED);
          }
          annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
        }
        Element element = TreeUtils.elementFromDeclaration(node);
        // check if is final and static
        if (Utility.isStaticAndFinal(element)) {
          ExpressionTree initializer = node.getInitializer();
          // check if initializer is a literal or a primitive
          if (Utility.isLiteralOrPrimitive(initializer)) {
            staticFinalVars.add((Symbol.VarSymbol) element);
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        }
      }
      return super.visitVariable(node, annotatedTypeMirror);
    }

    @Override
    public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      if (ENABLE_CUSTOM_CHECK) {
        annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
      }
      return super.visitLiteral(node, annotatedTypeMirror);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      if (ENABLE_CUSTOM_CHECK) {
        if (Utility.isEnumConstant(node)) {
          annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
        }
        Element element = TreeUtils.elementFromUse(node);
        // check if is final and static
        if (Utility.isStaticAndFinal(element) && element.getKind().isField()) {
          if (node instanceof JCTree.JCFieldAccess) {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        }
      }
      return super.visitMemberSelect(node, annotatedTypeMirror);
    }

    /**
     * Visits all new class creations and updates {@link AnnotatedTypeMirror} according to the
     * argument and receiver annotations. If any of the arguments or the receiver is {@link
     * RTainted}, the {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
     *
     * @param node the node being visited
     * @param annotatedTypeMirror annotated type of the new class
     */
    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedTypeMirror) {
      if (ENABLE_CUSTOM_CHECK) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        if (!isInAnnotatedPackage(node) && !isPresentInStub(node)) {
          if (hasTaintedArgument(node) || hasTaintedReceiver(node)) {
            //            annotatedTypeMirror.replaceAnnotation(RTAINTED);
          } else {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        } else {
          if (!hasTaintedArgument(node)) {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        }
      }
      return super.visitNewClass(node, annotatedTypeMirror);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror mirror) {
      List<? extends ExpressionTree> initializers = node.getInitializers();
      boolean allUntainted = true;
      if (initializers != null) {
        for (ExpressionTree initializer : initializers) {
          if (mayBeTainted(initializer)) {
            allUntainted = false;
            break;
          }
        }
      }
      if (allUntainted) {
        mirror.replaceAnnotation(RUNTAINTED);
      }
      return super.visitNewArray(node, mirror);
    }
  }
}

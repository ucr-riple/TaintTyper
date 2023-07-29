package edu.ucr.cs.riple.taint.ucrtainting;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.EnumHandler;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.StaticFinalFieldHandler;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.ThirdPartyHandler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
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

  private final ImmutableSet<Handler> handlers;

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
    this.handlers =
        ImmutableSet.<Handler>builder()
            .add(
                new StaticFinalFieldHandler(this),
                new EnumHandler(this),
                new ThirdPartyHandler(this))
            .build();
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new UCRTaintingTreeAnnotator(this, handlers));
  }

  /**
   * Checks if any of the arguments of the node has been annotated with {@link RTainted}
   *
   * @param node to check for
   * @return true if any argument is annotated with {@link RTainted}, false otherwise
   */
  public boolean hasTaintedArgument(ExpressionTree node) {
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
  public boolean hasTaintedReceiver(ExpressionTree node) {
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
  public boolean isInAnnotatedPackage(ExpressionTree node) {
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
    super.addAnnotationsFromDefaultForType(element, type);
    if (ENABLE_CUSTOM_CHECK) {
      handlers.forEach(handler -> handler.addAnnotationsFromDefaultForType(element, type));
    }
  }

  public void makeUntainted(AnnotatedTypeMirror type) {
    type.replaceAnnotation(RUNTAINTED);
  }

  public boolean customCheckIsEnabled() {
    return ENABLE_CUSTOM_CHECK;
  }
}

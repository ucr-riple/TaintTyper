package edu.ucr.cs.riple.taint.ucrtainting;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CollectionHandler;
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
  public final boolean customCheckEnabled;
  /** List of annotated packages. Classes in these packages are considered to be annotated. */
  private final List<String> annotatedPackages;
  /** AnnotationMirror for {@link RUntainted}. */
  private final AnnotationMirror rUntainted;
  /** AnnotationMirror for {@link RTainted}. */
  private final AnnotationMirror rTainted;
  /** Set of handlers */
  private final ImmutableSet<Handler> handlers;

  private final Context context;

  public UCRTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    customCheckEnabled = checker.getBooleanOption(UCRTaintingChecker.ENABLE_CUSTOM_CHECKER, true);
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
    // To respect existing annotations, leave them alone based on the provided annotated
    // package names through this option.
    String annotatedPackagesFlagValue =
        givenAnnotatedPackages.equals("\"\"") ? "" : givenAnnotatedPackages;
    annotatedPackages = Arrays.asList(annotatedPackagesFlagValue.split(","));
    rUntainted = AnnotationBuilder.fromClass(elements, RUntainted.class);
    rTainted = AnnotationBuilder.fromClass(elements, RTainted.class);
    this.context = ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext();
    this.handlers =
        ImmutableSet.<Handler>builder()
            .add(
                new StaticFinalFieldHandler(this),
                new EnumHandler(this),
                new ThirdPartyHandler(this),
                new CollectionHandler(this, context))
            .build();
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new UCRTaintingTreeAnnotator(this, handlers, context));
  }

  @Override
  protected void addAnnotationsFromDefaultForType(
      @Nullable Element element, AnnotatedTypeMirror type) {
    super.addAnnotationsFromDefaultForType(element, type);
    if (customCheckEnabled) {
      handlers.forEach(handler -> handler.addAnnotationsFromDefaultForType(element, type));
    }
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
        if (mayBeTainted(getAnnotatedType(eTree))) {
          return true;
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
          return modifiers != null
              && !modifiers.contains(Modifier.STATIC)
              && getAnnotatedType(receiverTree).hasAnnotation(rTainted);
        }
      }
    }
    return false;
  }

  /**
   * Checks if the package for the tree is present in already annotated according to provided
   * option.
   *
   * @param tree to check for
   * @return true if present, false otherwise
   */
  public boolean isInThirdPartyCode(Tree tree) {
    return isInThirdPartyCode(TreeUtils.elementFromTree(tree));
  }

  public boolean isInThirdPartyCode(Element element) {
    if (element == null) {
      return false;
    }
    Symbol symbol = (Symbol) element;
    return !Utility.isInAnnotatedPackage(symbol, this);
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
      return elem != null && isFromStubFile(elem);
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
    for (String annotatedPackageName : annotatedPackages) {
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
    return !type.hasAnnotation(rUntainted);
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

  /**
   * Checks if the given annotated type mirror has the {@link RUntainted} annotation.
   *
   * @param type The given annotated type mirror
   * @return True if the given annotated type mirror has the {@link RUntainted} annotation, false
   *     otherwise.
   */
  public boolean hasUntaintedAnnotation(AnnotatedTypeMirror type) {
    return type.hasAnnotation(rUntainted);
  }

  /**
   * Makes the given type {@link RUntainted}.
   *
   * @param type The given type.
   */
  public void makeUntainted(AnnotatedTypeMirror type) {
    type.replaceAnnotation(rUntainted);
  }

  /**
   * Checks if custom check is enabled.
   *
   * @return True if custom check is enabled, false otherwise.
   */
  public boolean customCheckIsEnabled() {
    return customCheckEnabled;
  }
}

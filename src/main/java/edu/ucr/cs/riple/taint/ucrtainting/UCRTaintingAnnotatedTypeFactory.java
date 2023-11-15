package edu.ucr.cs.riple.taint.ucrtainting;

import static edu.ucr.cs.riple.taint.ucrtainting.Log.print;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CompositHandler;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

public class UCRTaintingAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /**
   * This option enables custom handling of third party code. By default, such handling is enabled.
   */
  public final boolean enableLibraryCheck;

  /**
   * This option enables custom handling of validation code. By default, such handling is disabled.
   */
  public final boolean enableValidationCheck;

  /**
   * To respect existing annotations, leave them alone based on the provided annotated package names
   * through this option.
   */
  private final List<String> listOfAnnotatedPackageNames;

  public final boolean enableSideEffect;

  /** AnnotationMirror for {@link RUntainted}. */
  public final AnnotationMirror rUntainted;
  /** AnnotationMirror for {@link RTainted}. */
  public final AnnotationMirror rTainted;
  /** AnnotationMirror for {@link RPolyTainted}. */
  public final AnnotationMirror rPolyTainted;

  private final Handler handler;

  public UCRTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, RPossiblyValidated.class, RUntainted.class, null);
    enableLibraryCheck = checker.hasOption(UCRTaintingChecker.ENABLE_LIBRARY_CHECKER);
    enableValidationCheck = checker.hasOption(UCRTaintingChecker.ENABLE_VALIDATION_CHECKER);
    enableSideEffect = checker.hasOption(UCRTaintingChecker.ENABLE_SIDE_EFFECT);
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
    this.listOfAnnotatedPackageNames = Arrays.asList(annotatedPackagesFlagValue.split(","));
    print(
        "Configuration: Annotated Packages: "
            + listOfAnnotatedPackageNames
            + ", Library Check Activation: "
            + enableLibraryCheck
            + ", Validation Check Activation: "
            + enableValidationCheck
            + ", Side Effect Activation: "
            + enableSideEffect);
    this.rUntainted = AnnotationBuilder.fromClass(elements, RUntainted.class);
    this.rTainted = AnnotationBuilder.fromClass(elements, RTainted.class);
    this.rPolyTainted = AnnotationBuilder.fromClass(elements, RPolyTainted.class);
    this.handler =
        new CompositHandler(
            this, ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext());
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new UCRTaintingTreeAnnotator(this, handler, (UCRTaintingChecker) checker));
  }

  @Override
  protected void addAnnotationsFromDefaultForType(
      @Nullable Element element, AnnotatedTypeMirror type) {
    super.addAnnotationsFromDefaultForType(element, type);
    if (enableLibraryCheck) {
      handler.addAnnotationsFromDefaultForType(element, type);
    }
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new UCRTaintingQualifierHierarchy(this.getSupportedTypeQualifiers(), this.elements);
  }

  public AnnotationMirror rPossiblyValidatedAM(List<String> calledMethods) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, RPossiblyValidated.class);
    builder.setValue("value", calledMethods.toArray());
    return builder.build();
  }

  /**
   * Replaces all existing {@link RPolyTainted} annotations with {@link RUntainted} annotation.
   *
   * @param annotatedTypeMirror Annotated type mirror whose {@link RPolyTainted} annotations are to
   *     be replaced.
   */
  public void replacePolyWithUntainted(AnnotatedTypeMirror annotatedTypeMirror) {
    if (annotatedTypeMirror.hasPrimaryAnnotation(rPolyTainted)) {
      annotatedTypeMirror.replaceAnnotation(rUntainted);
    }
    if (annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      ((AnnotatedTypeMirror.AnnotatedDeclaredType) annotatedTypeMirror)
          .getTypeArguments()
          .forEach(this::replacePolyWithUntainted);
    }
  }

  public void replacePolyWithUntainted(
      AnnotatedTypeMirror toAdaptType, AnnotatedTypeMirror typeWithPoly) {
    if (typeWithPoly.hasPrimaryAnnotation(rPolyTainted)) {
      toAdaptType.replaceAnnotation(rUntainted);
    }
    if (toAdaptType instanceof AnnotatedTypeMirror.AnnotatedDeclaredType
        && typeWithPoly instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      List<AnnotatedTypeMirror> toAdaptTypeArgs =
          ((AnnotatedTypeMirror.AnnotatedDeclaredType) toAdaptType).getTypeArguments();
      List<AnnotatedTypeMirror> typeWithPolyArgs =
          ((AnnotatedTypeMirror.AnnotatedDeclaredType) typeWithPoly).getTypeArguments();
      for (int i = 0; i < toAdaptTypeArgs.size(); i++) {
        replacePolyWithUntainted(toAdaptTypeArgs.get(i), typeWithPolyArgs.get(i));
      }
    }
  }

  private class UCRTaintingQualifierHierarchy extends AccumulationQualifierHierarchy {

    /**
     * Creates a ElementQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    protected UCRTaintingQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
    }

    @Override
    public AnnotationMirror greatestLowerBoundQualifiers(
        final AnnotationMirror a1, final AnnotationMirror a2) {
      if (AnnotationUtils.areSame(a1, bottom) || AnnotationUtils.areSame(a2, bottom)) {
        return bottom;
      }

      if (AnnotationUtils.areSame(a1, rTainted)) {
        return a2;
      } else if (AnnotationUtils.areSame(a2, rTainted)) {
        return a1;
      }

      if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
        return a1;
      } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
        return bottom;
      }

      // If either is a predicate, then both should be converted to predicates and and-ed.
      if (isPredicate(a1) || isPredicate(a2)) {
        String a1Pred = convertToPredicate(a1);
        String a2Pred = convertToPredicate(a2);
        // check for top
        if (a1Pred.isEmpty()) {
          return a2;
        } else if (a2Pred.isEmpty()) {
          return a1;
        } else {
          return createPredicateAnnotation("(" + a1Pred + ") && (" + a2Pred + ")");
        }
      }

      List<String> a1Val = getAccumulatedValues(a1);
      List<String> a2Val = getAccumulatedValues(a2);
      // Avoid creating new annotation objects in the common case.
      if (a1Val.containsAll(a2Val)) {
        return a1;
      }
      if (a2Val.containsAll(a1Val)) {
        return a2;
      }
      a1Val.addAll(a2Val); // union
      return createAccumulatorAnnotation(a1Val);
    }

    /**
     * LUB in this type system is set intersection of the arguments of the two annotations, unless
     * one of them is bottom, in which case the result is the other annotation.
     */
    @Override
    public AnnotationMirror leastUpperBoundQualifiers(
        final AnnotationMirror a1, final AnnotationMirror a2) {
      if (AnnotationUtils.areSame(a1, bottom)) {
        return a2;
      } else if (AnnotationUtils.areSame(a2, bottom)) {
        return a1;
      }

      if (AnnotationUtils.areSame(a1, rTainted)) {
        return rTainted;
      } else if (AnnotationUtils.areSame(a2, rTainted)) {
        return rTainted;
      }

      if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
        return a1;
      } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
        return top;
      }

      // If either is a predicate, then both should be converted to predicates and or-ed.
      if (isPredicate(a1) || isPredicate(a2)) {
        String a1Pred = convertToPredicate(a1);
        String a2Pred = convertToPredicate(a2);
        // check for top
        if (a1Pred.isEmpty()) {
          return a1;
        } else if (a2Pred.isEmpty()) {
          return a2;
        } else {
          return createPredicateAnnotation("(" + a1Pred + ") || (" + a2Pred + ")");
        }
      }

      List<String> a1Val = getAccumulatedValues(a1);
      List<String> a2Val = getAccumulatedValues(a2);
      // Avoid creating new annotation objects in the common case.
      if (a1Val.containsAll(a2Val)) {
        return a2;
      }
      if (a2Val.containsAll(a1Val)) {
        return a1;
      }
      a1Val.retainAll(a2Val); // intersection
      return createAccumulatorAnnotation(a1Val);
    }

    @Override
    public boolean isSubtypeQualifiers(
        final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
      if (AnnotationUtils.areSame(subAnno, superAnno)) {
        return true;
      }

      if (AnnotationUtils.areSame(subAnno, bottom)) {
        return true;
      } else if (AnnotationUtils.areSame(superAnno, bottom)) {
        return false;
      }

      if (AnnotationUtils.areSame(superAnno, rTainted)
          && AnnotationUtils.areSame(subAnno, rTainted)) {
        return true;
      }

      if (AnnotationUtils.areSame(subAnno, rTainted)) {
        return false;
      } else if (AnnotationUtils.areSame(superAnno, rTainted)) {
        return true;
      }

      if (isPolymorphicQualifier(subAnno)) {
        if (isPolymorphicQualifier(superAnno)) {
          return true;
        } else {
          // Use this slightly more expensive conversion here because this is a rare code
          // path, and it's simpler to read than checking for both predicate and
          // non-predicate forms of top.
          return "".equals(convertToPredicate(superAnno));
        }
      } else if (isPolymorphicQualifier(superAnno)) {
        // Polymorphic annotations are only a supertype of other polymorphic annotations and
        // the bottom type, both of which have already been checked above.
        return false;
      }

      if (isPredicate(subAnno)) {
        return isPredicateSubtype(convertToPredicate(subAnno), convertToPredicate(superAnno));
      } else if (isPredicate(superAnno)) {
        return evaluatePredicate(subAnno, convertToPredicate(superAnno));
      }

      List<String> subVal = getAccumulatedValues(subAnno);
      List<String> superVal = getAccumulatedValues(superAnno);
      return subVal.containsAll(superVal);
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
              && getAnnotatedType(receiverTree).hasPrimaryAnnotation(rTainted);
        }
      }
    }
    return false;
  }

  /**
   * Checks if the package for the node is present in already annotated according to provided
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
  public boolean isPresentInStub(Tree node) {
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
    for (String annotatedPackageName : listOfAnnotatedPackageNames) {
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
   * Checks if the given element is a source of taint.
   *
   * @param element The given element.
   * @return True if the given element is a source of taint, false otherwise.
   */
  public boolean isSource(Element element) {
    if (!isFromStubFile(element)) {
      return false;
    }
    AnnotatedTypeMirror typeMirror = stubTypes.getAnnotatedTypeMirror(element);
    if (typeMirror == null) {
      return false;
    }
    return hasTaintedAnnotation(typeMirror);
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
    return !hasUntaintedAnnotation(type);
  }

  /**
   * Checks if the given tree has the {@link RUntainted} annotation.
   *
   * @param tree The given tree
   * @return True if the given tree has the {@link RUntainted} annotation, false otherwise.
   */
  public boolean hasUntaintedAnnotation(Tree tree) {
    return hasUntaintedAnnotation(getAnnotatedType(tree));
  }

  /**
   * Checks if the given annotated type mirror has the {@link RUntainted} annotation.
   *
   * @param type The given annotated type mirror
   * @return True if the given annotated type mirror has the {@link RUntainted} annotation, false
   *     otherwise.
   */
  public boolean hasUntaintedAnnotation(AnnotatedTypeMirror type) {
    return type.hasPrimaryAnnotation(rUntainted);
  }

  public boolean hasTaintedAnnotation(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedTypeMirror.AnnotatedExecutableType) {
      return hasTaintedAnnotation(
          ((AnnotatedTypeMirror.AnnotatedExecutableType) type).getReturnType());
    }
    return type.hasPrimaryAnnotation(rTainted);
  }

  /**
   * Checks if the given tree has the {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted}
   * annotation.
   *
   * @param tree The given tree
   * @return True if the given tree has the {@link
   *     edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted} annotation, false otherwise.
   */
  public boolean hasPolyTaintedAnnotation(Tree tree) {
    return hasPolyTaintedAnnotation(getAnnotatedType(tree));
  }

  public boolean hasPolyTaintedAnnotation(Type type) {
    return type.getAnnotationMirrors().stream()
        .anyMatch(typeCompound -> typeCompound.type.tsym.name.toString().equals("RPolyTainted"));
  }

  /**
   * Checks if the given annotated type mirror has the {@link
   * edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted} annotation.
   *
   * @param type The given annotated type mirror
   * @return True if the given annotated type mirror has the {@link
   *     edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted} annotation, false otherwise.
   */
  public boolean hasPolyTaintedAnnotation(AnnotatedTypeMirror type) {
    return type.hasPrimaryAnnotation(rPolyTainted);
  }

  public boolean isPolyOrUntainted(Tree tree) {
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    return isPolyOrUntainted(type);
  }

  public boolean isPolyOrUntainted(AnnotatedTypeMirror type) {
    return hasPolyTaintedAnnotation(type) || !mayBeTainted(type);
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
   * Makes the given type and all it's including parameter types {@link RUntainted} recursively. If
   * the given type is an array type, the component type will also be made {@link RUntainted}.
   *
   * @param type The given type.
   */
  public void makeDeepUntainted(AnnotatedTypeMirror type) {
    makeUntainted(type);
    //    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
    //      makeDeepUntainted(((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType());
    //    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
      declaredType.getTypeArguments().forEach(this::makeDeepUntainted);
    }
  }

  public boolean isUnannotatedThirdParty(Tree tree) {
    return isInThirdPartyCode(tree) && !isPresentInStub(tree);
  }
  /**
   * Checks if custom check is enabled.
   *
   * @return True if custom check is enabled, false otherwise.
   */
  public boolean customLibraryCheckIsEnabled() {
    return enableLibraryCheck;
  }
}

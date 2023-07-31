package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.CompositHandler;
import edu.ucr.cs.riple.taint.ucrtainting.handlers.Handler;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;

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
    listOfAnnotatedPackageNames = Arrays.asList(annotatedPackagesFlagValue.split(","));
    rUntainted = AnnotationBuilder.fromClass(elements, RUntainted.class);
    rTainted = AnnotationBuilder.fromClass(elements, RTainted.class);
    this.handler = new CompositHandler(this);
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new UCRTaintingTreeAnnotator(
            this,
            handler,
            ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext()));
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
    AnnotationMirror am = builder.build();
    return am;
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
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
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
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
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
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
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
          // path and it's simpler to read than checking for both predicate and
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
              && getAnnotatedType(receiverTree).hasAnnotation(rTainted);
        }
      }
    }
    return false;
  }

  /**
   * Checks if the receiver tree is available
   *
   * @param node to check for
   * @return true if available, false otherwise
   */
  public boolean hasReceiver(ExpressionTree node) {
    if (node != null) {
      ExpressionTree receiverTree = TreeUtils.getReceiverTree(node);
      if (receiverTree != null) {
        return true;
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
   * Visits all method invocations and updates {@link AnnotatedTypeMirror} according to the argument
   * and receiver annotations. If any of the arguments or the receiver is {@link RTainted}, the
   * {@link AnnotatedTypeMirror} is updated to be {@link RTainted}.
   *
   * @param node the node being visited
   * @param annotatedTypeMirror annotated return type of the method invocation
   */

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

  public boolean isUnannotatedThirdParty(Tree tree) {
    if (isInThirdPartyCode(tree) && !isPresentInStub(tree)) {
      return true;
    }
    return false;
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

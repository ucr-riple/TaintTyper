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
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
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
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

public class UCRTaintingAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /**
   * This option enables custom handling of validation code. By default, such handling is disabled.
   */
  public final boolean enableValidationCheck;
  /**
   * To respect existing annotations, leave them alone based on the provided annotated package names
   * through this option.
   */
  private final List<String> listOfAnnotatedPackageNames;
  /**
   * This option enables custom handling of side effects of calling methods with tainted arguments
   * on the receiver. By default, such handling is disabled.
   */
  public final boolean enableSideEffect;
  /**
   * This option enables custom handling of third party code. By default, such handling is enabled.
   */
  public final boolean enableLibraryCheck;
  /**
   * This option enables inference of {@link RPolyTainted} annotations. By default, such inference
   * is disabled.
   */
  public final boolean enablePolyTaintInference;
  /** This option enables inference of type arguments. By default, such inference is disabled. */
  public final boolean enableTypeArgumentInference;
  /** AnnotationMirror for {@link RUntainted}. */
  public final AnnotationMirror rUntainted;
  /** AnnotationMirror for {@link RTainted}. */
  public final AnnotationMirror rTainted;
  /** AnnotationMirror for {@link RPolyTainted}. */
  public final AnnotationMirror rPolyTainted;
  /** The handler for the checker. Used to handle the custom logic of the checker. */
  private final Handler handler;

  public UCRTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, RPossiblyValidated.class, RUntainted.class, null);
    enableLibraryCheck = checker.getBooleanOption(UCRTaintingChecker.ENABLE_LIBRARY_CHECKER, true);
    enablePolyTaintInference =
        checker.getBooleanOption(UCRTaintingChecker.ENABLE_POLY_TAINT_INFERENCE, true);
    enableTypeArgumentInference =
        checker.getBooleanOption(UCRTaintingChecker.ENABLE_TYPE_ARGUMENT_INFERENCE, true);
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
            + ", Library: "
            + enableLibraryCheck
            + ", Poly: "
            + enablePolyTaintInference
            + ", Type Argument: "
            + enableTypeArgumentInference
            + ", Validation: "
            + enableValidationCheck
            + ", Side Effect: "
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
        super.createTreeAnnotator(), new UCRTaintingTreeAnnotator(this, handler));
  }

  @Override
  protected void addAnnotationsFromDefaultForType(
      @Nullable Element element, AnnotatedTypeMirror type) {
    super.addAnnotationsFromDefaultForType(element, type);
    // make reference type for var args untainted.
    if (element instanceof Symbol.VarSymbol) {
      if (((Symbol.VarSymbol) element).type instanceof Type.ArrayType) {
        if (((Type.ArrayType) ((Symbol.VarSymbol) element).type).isVarargs()) {
          makeUntainted(type);
        }
      }
    }
    handler.addAnnotationsFromDefaultForType(element, type);
  }

  @Override
  protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
    return super.createDefaultForUseTypeAnnotator();
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new UCRTaintingQualifierHierarchy(this.getSupportedTypeQualifiers(), this.elements);
  }

  @Override
  protected DefaultForTypeAnnotator createDefaultForTypeAnnotator() {
    return new UCRTaintingTypeAnnotator(this);
  }

  public AnnotationMirror rPossiblyValidatedAM(List<String> calledMethods) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, RPossiblyValidated.class);
    builder.setValue("value", calledMethods.toArray());
    return builder.build();
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
   * Checks if the passed method is in unannotated code.
   *
   * @param symbol Method symbol to check for.
   * @return true if in unannotated code, false otherwise
   */
  public boolean isUnannotatedMethod(Symbol.MethodSymbol symbol) {
    return isUnannotatedCodeSymbol(symbol);
  }

  /**
   * Checks if the passed field is in unannotated code.
   *
   * @param symbol Field symbol to check for.
   * @return true if in unannotated code, false otherwise
   */
  public boolean isUnannotatedField(Symbol.VarSymbol symbol) {
    return isUnannotatedCodeSymbol(symbol);
  }

  /**
   * Method to check if the passed symbol is in unannotated code. The method is private
   * intentionally to make sure the symbol is either a method or a field.
   *
   * @param symbol Symbol to check for.
   * @return true if in unannotated code, false otherwise
   */
  private boolean isUnannotatedCodeSymbol(Symbol symbol) {
    if (symbol == null) {
      return false;
    }
    if (isFromStubFile(symbol)) {
      return false;
    }
    String packageName = symbol.packge().getQualifiedName().toString();
    if (packageName.equals("unnamed package")) {
      packageName = "";
    }
    boolean isUnAnnotatedPackage = isUnAnnotatedPackageName(packageName);
    if (isUnAnnotatedPackage) {
      return true;
    }
    Path path = SymbolUtils.getPathFromSymbol(symbol);
    if (path == null) {
      return true;
    }
    return path.toString().contains("/generated-sources/");
  }

  /**
   * Checks if the package name matches any of the annotated packages.
   *
   * @param packageName to check for
   * @return false if the package name matches any of the annotated packages, true otherwise.
   */
  public boolean isUnAnnotatedPackageName(String packageName) {
    for (String annotatedPackageName : listOfAnnotatedPackageNames) {
      if (packageName.startsWith(annotatedPackageName)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Retrieves the effective annotated type of the given type. For most types that equals the type
   * itself, however, for arrays and wildcards, the component type and the extends bound are
   * returned respectively.
   *
   * @param type The type to get the effective annotated type of.
   * @return The effective annotated type of the given type.
   */
  private static AnnotatedTypeMirror getTargetType(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      return ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
    }
    if (type instanceof AnnotatedTypeMirror.AnnotatedWildcardType) {
      return ((AnnotatedTypeMirror.AnnotatedWildcardType) type).getExtendsBound();
    }
    return type;
  }

  /**
   * Retrieves the effective type of the given type. For most types that equals the type itself,
   * however, for arrays and wildcards, the component type and the extends bound are returned
   * respectively.
   *
   * @param type The type to get the effective type of.
   * @return The effective type of the given type.
   */
  private static Type getTargetType(Type type) {
    if (type instanceof Type.ArrayType) {
      return ((Type.ArrayType) type).getComponentType();
    }
    if (type instanceof Type.WildcardType) {
      return ((Type.WildcardType) type).getExtendsBound();
    }
    return type;
  }

  /**
   * Replaces all existing {@link RPolyTainted} annotations with {@link RUntainted} annotation.
   *
   * @param annotatedTypeMirror Annotated type mirror whose {@link RPolyTainted} annotations are to
   *     be replaced.
   */
  public void replacePolyWithUntainted(AnnotatedTypeMirror annotatedTypeMirror) {
    if (hasPolyTaintedAnnotation(annotatedTypeMirror)) {
      annotatedTypeMirror.replaceAnnotation(rUntainted);
    }
    if (annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      ((AnnotatedTypeMirror.AnnotatedDeclaredType) annotatedTypeMirror)
          .getTypeArguments()
          .forEach(this::replacePolyWithUntainted);
    }
    if (annotatedTypeMirror instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      replacePolyWithUntainted(
          ((AnnotatedTypeMirror.AnnotatedArrayType) annotatedTypeMirror).getComponentType());
    }
  }

  /**
   * Replaces all existing {@link RPolyTainted} annotations with {@link RUntainted} annotation
   * according to the typeWithPoly. (e.g. {@code List<String>} as toAdaptType and {@code
   * List<@RPolyTainted String>} as typeWithPoly will be converted to {@code List<@RUntainted
   * String>})
   *
   * @param toAdaptType The type to be adapted.
   * @param typeWithPoly The type with {@link RPolyTainted} annotations.
   */
  public void replacePolyWithUntainted(
      AnnotatedTypeMirror toAdaptType, AnnotatedTypeMirror typeWithPoly) {
    if (hasPolyTaintedAnnotation(typeWithPoly)) {
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

  /**
   * Makes the type argument at the given position untainted.
   *
   * @param type The type to be adapted.
   * @param positions The positions of the type arguments to be adapted.
   */
  public void makeUntainted(AnnotatedTypeMirror type, Set<TypeIndex> positions) {
    if (positions.isEmpty()) {
      return;
    }
    positions.forEach(integers -> makeUntaintedForPosition(type, integers, 0));
  }

  /**
   * Makes the type argument at the given position untainted.
   *
   * @param type The type to be adapted.
   * @param position The position of the type argument to be adapted.
   * @param index The index of the position.
   */
  private void makeUntaintedForPosition(AnnotatedTypeMirror type, TypeIndex position, int index) {
    // TODO: This method can be rewritten to remove index parameter.
    if (index == position.size()) {
      return;
    }
    if (position.get(index) == 0) {
      makeUntainted(type);
      return;
    }
    if (!(type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
    int typeArgPosition = position.get(index) - 1;
    makeUntaintedForPosition(
        declaredType.getTypeArguments().get(typeArgPosition), position, index + 1);
  }

  /**
   * Makes the given type poly-tainted.
   *
   * @param type The type to be adapted.
   */
  public void makePolyTainted(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      // We don't want to make the array type poly-tainted, but rather the component type.
      type = ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
    }
    type.replaceAnnotation(rPolyTainted);
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
    return !hasUntaintedAnnotation(type) && !hasPolyTaintedAnnotation(type);
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
    type = getTargetType(type);
    return type.hasPrimaryAnnotation(rUntainted);
  }

  public boolean hasPolyTaintedAnnotation(Type type) {
    type = getTargetType(type);
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
    type = getTargetType(type);
    return type.hasPrimaryAnnotation(rPolyTainted);
  }

  /**
   * Checks if the given tree has the {@link RPolyTainted} or {@link RUntainted} annotation.
   *
   * @param tree The given tree.
   * @return True if the given tree has the {@link RPolyTainted} or {@link RUntainted} annotation.
   */
  public boolean isPolyOrUntainted(Tree tree) {
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    return isPolyOrUntainted(type);
  }

  /**
   * Checks if the given annotated type mirror is {@link RUntainted} or {@link RPolyTainted}.
   *
   * @param type The given annotated type mirror.
   * @return True if the given annotated type mirror is {@link RUntainted} or {@link RPolyTainted}.
   */
  public boolean isPolyOrUntainted(AnnotatedTypeMirror type) {
    return hasPolyTaintedAnnotation(type) || !mayBeTainted(type);
  }

  /**
   * Makes the given type {@link RUntainted}.
   *
   * @param type The given type.
   */
  public void makeUntainted(AnnotatedTypeMirror type) {
    //    type = getTargetType(type);
    type.replaceAnnotation(rUntainted);
  }

  /**
   * Makes the given type and all it's including type arguments {@link RUntainted} recursively.
   * Please note that this method does not make component types of arrays untainted. Making that
   * untainted will introduce huge number of false positives.
   *
   * @param type The given type to make untainted.
   */
  public void makeDeepUntainted(AnnotatedTypeMirror type) {
    makeUntainted(type);
    if (type instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
      declaredType.getTypeArguments().forEach(this::makeDeepUntainted);
    }
  }

  /**
   * Checks if custom check is enabled.
   *
   * @return True if custom check is enabled, false otherwise.
   */
  public boolean libraryCheckIsEnabled() {
    return enableLibraryCheck;
  }

  /**
   * Check if poly taint inference is enabled.
   *
   * @return True if poly taint inference is enabled, false otherwise.
   */
  public boolean polyTaintInferenceEnabled() {
    return enablePolyTaintInference;
  }

  /**
   * Check if type argument inference is enabled.
   *
   * @return True if type argument inference is enabled, false otherwise.
   */
  public boolean typeArgumentInferenceEnabled() {
    return enableTypeArgumentInference;
  }

  public boolean allTypeArgumentsAreSubType(
      AnnotatedTypeMirror found, AnnotatedTypeMirror required) {
    if (found instanceof AnnotatedTypeMirror.AnnotatedDeclaredType
        && required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      AnnotatedTypeMirror.AnnotatedDeclaredType foundType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) found;
      AnnotatedTypeMirror.AnnotatedDeclaredType requiredType =
          (AnnotatedTypeMirror.AnnotatedDeclaredType) required;
      if (foundType.getTypeArguments().isEmpty() && requiredType.getTypeArguments().isEmpty()) {
        AnnotatedTypeMirror widenedValueType = getWidenedType(found, required);
        try {
          return getTypeHierarchy().isSubtype(widenedValueType, required);
        } catch (Exception e) {
          return false;
        }
      }

      List<AnnotatedTypeMirror> foundTypeArgs =
          ((AnnotatedTypeMirror.AnnotatedDeclaredType) found).getTypeArguments();
      List<AnnotatedTypeMirror> requiredTypeArgs =
          ((AnnotatedTypeMirror.AnnotatedDeclaredType) required).getTypeArguments();
      if (foundTypeArgs.size() != requiredTypeArgs.size()) {
        return false;
      }
      for (int i = 0; i < foundTypeArgs.size(); i++) {
        if (!allTypeArgumentsAreSubType(foundTypeArgs.get(i), requiredTypeArgs.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
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
}

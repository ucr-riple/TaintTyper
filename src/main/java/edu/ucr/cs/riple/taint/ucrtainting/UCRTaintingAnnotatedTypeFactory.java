package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RPossiblyValidated;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UCRTaintingAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /**
   * This option enables custom handling of third party code. By default, such handling is enabled.
   */
  public final boolean ENABLE_LIBRARY_CHECK;

  public final boolean ENABLE_VALIDATION_CHECK;

  public final boolean ENABLE_SANITIZER_CHECK;
  /**
   * To respect existing annotations, leave them alone based on the provided annotated package names
   * through this option.
   */
  private final String ANNOTATED_PACKAGE_NAMES;
  /** List of annotated packages. Classes in these packages are considered to be annotated. */
  private final List<String> ANNOTATED_PACKAGE_NAMES_LIST;
  /** AnnotationMirror for {@link RUntainted}. */
  public final AnnotationMirror RUNTAINTED;
  /** AnnotationMirror for {@link RTainted}. */
  public final AnnotationMirror RTAINTED;

  public UCRTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, RPossiblyValidated.class, RUntainted.class, null);
    ENABLE_LIBRARY_CHECK =
        checker.getBooleanOption(UCRTaintingChecker.ENABLE_LIBRARY_CHECKER, true);
    ENABLE_VALIDATION_CHECK =
        checker.getBooleanOption(UCRTaintingChecker.ENABLE_VALIDATION_CHECKER, false);
    ENABLE_SANITIZER_CHECK =
        checker.getBooleanOption(UCRTaintingChecker.ENABLE_SANITIZATION_CHECKER, false);
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
    ANNOTATED_PACKAGE_NAMES = givenAnnotatedPackages.equals("\"\"") ? "" : givenAnnotatedPackages;
    ANNOTATED_PACKAGE_NAMES_LIST = Arrays.asList(ANNOTATED_PACKAGE_NAMES.split(","));
    RUNTAINTED = AnnotationBuilder.fromClass(elements, RUntainted.class);
    RTAINTED = AnnotationBuilder.fromClass(elements, RTainted.class);
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new UCRTaintingTreeAnnotator(this));
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

      if (AnnotationUtils.areSame(a1, RTAINTED)) {
        return a2;
      } else if (AnnotationUtils.areSame(a2, RTAINTED)) {
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

      if (AnnotationUtils.areSame(a1, RTAINTED)) {
        return RTAINTED;
      } else if (AnnotationUtils.areSame(a2, RTAINTED)) {
        return RTAINTED;
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

      if (AnnotationUtils.areSame(superAnno, RTAINTED)
          && AnnotationUtils.areSame(subAnno, RTAINTED)) {
        return true;
      }

      if (AnnotationUtils.areSame(subAnno, RTAINTED)) {
        return false;
      } else if (AnnotationUtils.areSame(superAnno, RTAINTED)) {
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
   * @param node to check for
   * @return true if present, false otherwise
   */
  public boolean hasAnnotatedPackage(ExpressionTree node) {
    if (node != null) {
      ExpressionTree receiverTree = TreeUtils.getReceiverTree(node);
      if (receiverTree != null) {
        String packageName = "";
        try {
          packageName = ElementUtils.getType(TreeUtils.elementFromTree(receiverTree)).toString();
          if (!packageName.equals("")) {
            packageName = packageName.substring(0, packageName.lastIndexOf("."));
          }
        } catch (Exception | Error e) {
          // TODO:: take care of exceptions or errors
        }
        if (isAnnotatedPackage(packageName)) {
          return true;
        }
      }
    }
    return false;
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

  public boolean returnsThis(MethodInvocationTree tree) {
    return this.getDeclAnnotation(TreeUtils.elementFromUse(tree), This.class) != null;
  }

  /**
   * Checks if the given tree may be tainted.
   *
   * @param tree The given tree.
   * @return True if the given tree is tainted, false otherwise.
   */
  public boolean mayBeTainted(Tree tree) {
    AnnotatedTypeMirror type = getAnnotatedType(tree);
    // If type is null, we should be conservative and assume it may be tainted.
    return type == null || !type.hasAnnotation(RUNTAINTED);
  }

  private class UCRTaintingTreeAnnotator extends TreeAnnotator {

    /**
     * UCRTaintingTreeAnnotator
     *
     * @param atypeFactory the type factory
     */
    protected UCRTaintingTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
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
      if (ENABLE_SANITIZER_CHECK) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        if (!hasAnnotatedPackage(node) && !isPresentInStub(node)) {
          if (!hasReceiver(node) && node.getArguments().size() == 1) {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          } else {

          }
        }
      }
      if (ENABLE_LIBRARY_CHECK) {
        // ignore local methods
        if (hasReceiver(node)) {
          // if the code is part of provided annotated packages or is present
          // in the stub files, then we don't need any custom handling for it.
          if (!hasAnnotatedPackage(node) && !isPresentInStub(node)) {
            if (!hasTaintedArgument(node) && !hasTaintedReceiver(node)) {
              annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
            }
          } else {
            if(isPresentInStub(node)) {
              if(returnsThis(node)) {
                System.out.println();
              }
            }
          }
        }
      }

      return super.visitMethodInvocation(node, annotatedTypeMirror);
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
      if (ENABLE_SANITIZER_CHECK) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        if (!hasAnnotatedPackage(node) && !isPresentInStub(node)) {
          if (!hasReceiver(node) && node.getArguments().size() == 1) {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        }
      }
      if (ENABLE_LIBRARY_CHECK) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        if (!hasAnnotatedPackage(node) && !isPresentInStub(node)) {
          if (!hasTaintedArgument(node) && !hasTaintedReceiver(node)) {
            annotatedTypeMirror.replaceAnnotation(RUNTAINTED);
          }
        }
      }
      return super.visitNewClass(node, annotatedTypeMirror);
    }
  }
}

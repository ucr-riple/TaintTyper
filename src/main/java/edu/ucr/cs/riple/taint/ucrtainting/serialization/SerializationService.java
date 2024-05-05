package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingVisitor;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.PolyMethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixComputer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.TypeMatchVisitor;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** This class is used to serialize the errors and the fixes for the errors. */
public class SerializationService {

  /** Serializer for the checker. */
  private final Serializer serializer;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if
   * the tree is {@link RTainted}
   */
  private final UCRTaintingAnnotatedTypeFactory typeFactory;
  /** Using checker instance. */
  private final UCRTaintingChecker checker;
  /** Javac context instance. */
  private final Context context;
  /** Fix computer to generate the required fixes for the errors. */
  private final FixComputer fixComputer;
  /**
   * Type match visitor to compute the required {@link
   * edu.ucr.cs.riple.taint.ucrtainting.qual.RUntainted} annotations to match the found type to
   * required type.
   */
  private final TypeMatchVisitor untaintedTypeMatchVisitor;
  /**
   * Type match visitor to compute the required {@link
   * edu.ucr.cs.riple.taint.ucrtainting.qual.RPolyTainted} annotations to match the found type to
   * required type.
   */
  private final TypeMatchVisitor polyTypeMatchVisitor;
  /** Javac types instance. */
  private final Types types;

  public SerializationService(UCRTaintingChecker checker) {
    this.checker = checker;
    this.serializer = new Serializer(checker);
    this.typeFactory = (UCRTaintingAnnotatedTypeFactory) checker.getTypeFactory();
    this.context = ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext();
    this.types = Types.instance(context);
    this.untaintedTypeMatchVisitor = TypeMatchVisitor.createUntaintedMatcher(typeFactory);
    this.polyTypeMatchVisitor = TypeMatchVisitor.createPolyTaintedMatcher(typeFactory);
    this.fixComputer = new FixComputer(typeFactory, types, context);
  }

  /**
   * This method is called when a warning or error is reported by the checker and serialized the
   * error along the set of required fixes to resolve the error if exists.
   *
   * @param source the source of the error
   * @param messageKey the key of the error message
   * @param pair the pair of found and required annotated type mirrors.
   */
  public void serializeError(Object source, String messageKey, FoundRequired pair) {
    if (serializer.isDisabled()) {
      return;
    }
    fixComputer.reset(checker.getVisitor().getCurrentPath());
    Set<Fix> resolvingFixes;
    try {
      resolvingFixes =
          checkErrorIsFixable(source, messageKey)
              ? generateFixesForError((Tree) source, messageKey, pair)
              : ImmutableSet.of();
    } catch (Exception e) {
      System.err.println(
          "Error in computing required fixes: " + source + " " + messageKey + ", exception:" + e);
      resolvingFixes = ImmutableSet.of();
      e.printStackTrace();
    }
    if (!typeFactory.typeArgumentInferenceEnabled()) {
      boolean inferredOnTypeArg =
          resolvingFixes.stream()
              .anyMatch(fix -> !fix.location.getTypeIndexSet().equals(TypeIndex.topLevel()));
      if (inferredOnTypeArg) {
        resolvingFixes = ImmutableSet.of();
      }
    }
    Error error = new Error(messageKey, resolvingFixes, checker.getVisitor().getCurrentPath());
    serializer.serializeError(error);
  }

  /**
   * Generates the fixes for the given tree if exists.
   *
   * @param tree The given tree.
   * @param messageKey The key of the error message.
   */
  public Set<Fix> generateFixesForError(Tree tree, String messageKey, FoundRequired pair) {
    TreePath path = checker.getVisitor().getCurrentPath();
    switch (messageKey) {
      case "override.param":
        return handleParamOverrideError(tree, pair);
      case "override.return":
        return handleReturnOverrideError(path.getLeaf());
      case "enhancedfor":
        {
          pair = updateFoundRequiredPairEnhancedForLoopError(tree, pair);
        }
      default:
        // On Right Hand Side
        Set<Fix> fixes = new HashSet<>(tree.accept(fixComputer, pair));
        // On Left Hand Side
        Set<Fix> onLeftHandSide = generateLeftHandSideFixes(tree, messageKey, path, pair);
        if (!onLeftHandSide.isEmpty()) {
          // let right hand side get fixed in the next iteration.
          return onLeftHandSide;
        }
        return fixes;
    }
  }

  /**
   * Generates the fixes for the left hand side of the type mismatch in assignment. The suggested
   * fixes should only be on the type arguments of the left hand side of the assignment.
   *
   * @param tree The tree to generate the fixes for.
   * @param messageKey The key of the error message.
   * @param path The path of the tree.
   * @param pair The pair of found and required annotated type mirrors.
   * @return The set of fixes to be applied to the tree.
   */
  private ImmutableSet<Fix> generateLeftHandSideFixes(
      Tree tree, String messageKey, TreePath path, FoundRequired pair) {
    if (!(pair.found instanceof AnnotatedTypeMirror.AnnotatedDeclaredType
        && pair.required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType)) {
      return ImmutableSet.of();
    }
    Element toAnnotate = null;
    switch (messageKey) {
      case "enhancedfor":
        toAnnotate =
            TreeUtils.elementFromDeclaration(((EnhancedForLoopTree) path.getLeaf()).getVariable());
        break;
      case "assignment":
        if (path.getLeaf() instanceof VariableTree) {
          toAnnotate = TreeUtils.elementFromTree((path.getLeaf()));
        }
        if (path.getLeaf() instanceof AssignmentTree) {
          toAnnotate = TreeUtils.elementFromTree(((AssignmentTree) path.getLeaf()).getVariable());
        }
        break;
      case "return":
        MethodTree enclosingMethod = SymbolUtils.findEnclosingNode(path, MethodTree.class);
        if (enclosingMethod == null) {
          return ImmutableSet.of();
        }
        toAnnotate = TreeUtils.elementFromDeclaration(enclosingMethod);
        break;
      case "argument":
        List<? extends ExpressionTree> args = SymbolUtils.getCallableArguments(path.getLeaf());
        int index = -1;
        for (int i = 0; i < args.size(); i++) {
          if (args.get(i).equals(tree)) {
            index = i;
            break;
          }
        }
        if (index == -1) {
          return ImmutableSet.of();
        }
        toAnnotate = SymbolUtils.getCallableArgumentsSymbol(path.getLeaf()).get(index);
        break;
      default:
        return ImmutableSet.of();
    }
    if (toAnnotate == null) {
      return ImmutableSet.of();
    }
    TypeMatchVisitor visitor = TypeMatchVisitor.createUntaintedMatcher(typeFactory);
    Set<TypeIndex> differences;
    try {
      // todo: for now we ignore the possible exceptions thrown by the visitor.
      differences = visitor.visit(pair.required, pair.found, null);
    } catch (Exception e) {
      return ImmutableSet.of();
    }
    if (!differences.isEmpty()) {
      // Remove top level difference as it is not a type argument.
      differences.remove(TypeIndex.TOP_LEVEL);
    }
    if (differences.isEmpty()) {
      return ImmutableSet.of();
    }
    Fix fixOnLeftHandSide = new Fix(SymbolLocation.createLocationFromSymbol((Symbol) toAnnotate));
    fixOnLeftHandSide.location.setTypeIndexSet(differences);
    return ImmutableSet.of(fixOnLeftHandSide);
  }

  /**
   * Updates the found and required pair for the enhanced for loop error. This method computes the
   * required {@link java.util.Iterator} type from the used collection in the loop which it entries
   * match the required type. Once the iterator type is computed, it generates a new instance of
   * {@link FoundRequired} pair corresponding to the iterator type and the required type for the
   * passed collection.
   *
   * @param iterationTree The tree used in the iteration.
   * @param pair The found and required pair for the iteration variable.
   * @return The updated pair if the found type is not a subtype of the required type, null
   */
  private FoundRequired updateFoundRequiredPairEnhancedForLoopError(
      Tree iterationTree, FoundRequired pair) {
    Set<TypeIndex> differences = untaintedTypeMatchVisitor.visit(pair.found, pair.required, null);
    if (differences.isEmpty()) {
      // In this case, the problem is on the left hand side of the assignment: e.g. List<String> l :
      // Iterator<List<@RUntainted String>> and the pair does not need to be translated to
      // collection type.
      return pair;
    }
    AnnotatedTypeMirror contentFoundType = typeFactory.getAnnotatedType(iterationTree);
    AnnotatedTypeMirror required = contentFoundType.deepCopy(true);
    if (required instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
      typeFactory.makeUntainted(
          ((AnnotatedTypeMirror.AnnotatedArrayType) required).getComponentType(), differences);
    }
    if (required instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
      typeFactory.makeUntainted(
          ((AnnotatedTypeMirror.AnnotatedDeclaredType) required).getTypeArguments().get(0),
          differences);
    }
    return FoundRequired.of(contentFoundType, required, 0);
  }

  /**
   * Computes the required fixes for wrong parameter override errors (type="override.param").
   *
   * @param paramTree the parameter tree.
   * @return the set of required fixes to resolve errors of type="override.param".
   */
  private ImmutableSet<Fix> handleParamOverrideError(Tree paramTree, FoundRequired pair) {
    Element treeElement = TreeUtils.elementFromTree(paramTree);
    if (treeElement == null) {
      return ImmutableSet.of();
    }
    Symbol.MethodSymbol overridingMethod = (Symbol.MethodSymbol) treeElement.getEnclosingElement();
    if (overridingMethod == null) {
      return ImmutableSet.of();
    }
    Types types = Types.instance(context);
    Symbol.MethodSymbol overriddenMethod =
        SymbolUtils.getClosestOverriddenMethod(overridingMethod, types);
    if (overriddenMethod == null) {
      return ImmutableSet.of();
    }
    int paramIndex = overridingMethod.getParameters().indexOf((Symbol.VarSymbol) treeElement);
    Symbol toBeAnnotated = overriddenMethod.getParameters().get(paramIndex);
    SymbolLocation location = SymbolLocation.createLocationFromSymbol(toBeAnnotated);
    if (location != null) {
      Set<TypeIndex> differences = untaintedTypeMatchVisitor.visit(pair.found, pair.required, null);
      if (differences.isEmpty()) {
        return ImmutableSet.of();
      }
      location.setTypeIndexSet(untaintedTypeMatchVisitor.visit(pair.found, pair.required, null));
    }
    return ImmutableSet.of(new Fix(location));
  }

  /**
   * Computes the required fixes for wrong return override errors (type="override.return").
   *
   * @return the set of required fixes to resolve errors of type="override.return".
   */
  private ImmutableSet<Fix> handleReturnOverrideError(Tree overridingMethodTree) {
    ImmutableSet.Builder<Fix> ans = new ImmutableSet.Builder<>();
    // On child
    Symbol.MethodSymbol overridingMethod =
        (Symbol.MethodSymbol) TreeUtils.elementFromTree(overridingMethodTree);
    if (overridingMethod == null) {
      return ImmutableSet.of();
    }
    // On parent
    ExecutableElement methodElement =
        TreeUtils.elementFromDeclaration((MethodTree) overridingMethodTree);
    AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
        ((UCRTaintingVisitor) checker.getVisitor())
            .getAnnotatedTypeOfOverriddenMethod(methodElement);
    Symbol.MethodSymbol overriddenMethod =
        SymbolUtils.getClosestOverriddenMethod(overridingMethod, types);
    if (overriddenType == null) {
      overriddenType = typeFactory.getAnnotatedType(overriddenMethod);
    }
    AnnotatedTypeMirror overriddenReturnType = overriddenType.getReturnType();
    AnnotatedTypeMirror overridingReturnType =
        typeFactory.getAnnotatedType(overridingMethod).getReturnType();
    Set<TypeIndex> differences =
        untaintedTypeMatchVisitor.visit(overriddenReturnType, overridingReturnType, null);
    if (!differences.isEmpty()) {
      SymbolLocation location = SymbolLocation.createLocationFromSymbol(overriddenMethod);
      if (location == null) {
        return ImmutableSet.of();
      }
      location.setTypeIndexSet(differences);
      ans.add(new Fix(location));
    }
    differences = untaintedTypeMatchVisitor.visit(overridingReturnType, overriddenReturnType, null);
    if (!differences.isEmpty()) {
      SymbolLocation location = SymbolLocation.createLocationFromSymbol(overridingMethod);
      if (location == null) {
        return ImmutableSet.of();
      }
      location.setTypeIndexSet(differences);
      ans.add(new Fix(location));
    } else {
      Set<TypeIndex> onPoly =
          polyTypeMatchVisitor.visit(overridingReturnType, overriddenReturnType, null);
      if (!onPoly.isEmpty()) {
        Set<MethodParameterLocation> methodParameterLocations = new HashSet<>();
        MethodLocation location =
            (MethodLocation) SymbolLocation.createLocationFromSymbol(overridingMethod);
        if (location == null) {
          return ImmutableSet.of();
        }
        location.setTypeIndexSet(onPoly);
        for (int i = 0;
            i < typeFactory.getAnnotatedType(overridingMethod).getParameterTypes().size();
            i++) {
          AnnotatedTypeMirror typeVariable = overriddenType.getParameterTypes().get(i);
          Set<TypeIndex> differencesOnParam =
              polyTypeMatchVisitor.visit(
                  typeFactory.getAnnotatedType(overridingMethod).getParameterTypes().get(i),
                  typeVariable,
                  null);
          if (!differencesOnParam.isEmpty()) {
            MethodParameterLocation methodParameterLocation =
                (MethodParameterLocation)
                    SymbolLocation.createLocationFromSymbol(
                        overridingMethod.getParameters().get(i));
            if (methodParameterLocation == null) {
              continue;
            }
            methodParameterLocation.setTypeIndexSet(differencesOnParam);
            methodParameterLocations.add(methodParameterLocation);
          }
        }
        if (!methodParameterLocations.isEmpty()) {
          PolyMethodLocation polyMethodLocation =
              new PolyMethodLocation(location, methodParameterLocations);
          Fix polyFix = new Fix(polyMethodLocation);
          ans.add(polyFix.toPoly());
        }
      }
    }
    return ans.build();
  }

  /**
   * Checks if the error is fixable with annotation injections on the source code elements.
   *
   * @param source The source of the error.
   * @param messageKey The key of the error message.
   * @return True, if the error is fixable, false otherwise.
   */
  public static boolean checkErrorIsFixable(Object source, String messageKey) {
    if (!(source instanceof Tree)) {
      // For all cases where the source is not a tree, we return false for now.
      return false;
    }
    switch (messageKey) {
      case "override.param":
      case "override.return":
      case "assignment":
      case "return":
      case "argument":
      case "conditional":
      case "compound.assignment":
      case "enhancedfor":
      case "array.initializer":
      case "enum.declaration":
      case "switch.expression":
        return true;
      default:
        // TODO: investigate if there are other cases where the error is fixable.
        // For all other cases, return false.
        return false;
    }
  }
}

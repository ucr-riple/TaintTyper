package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import static edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility.getAnnotatedTypeMirrorOfTypeArgumentAt;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
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
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.PolyTypeMatchVisitor;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.TypeMatchVisitor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

  private final FixComputer fixComputer;
  private final TypeMatchVisitor typeMatchVisitor;
  private final TypeMatchVisitor polyTypeMatchVisitor;
  private final Types types;

  public SerializationService(UCRTaintingChecker checker) {
    this.checker = checker;
    this.serializer = new Serializer(checker);
    this.typeFactory = (UCRTaintingAnnotatedTypeFactory) checker.getTypeFactory();
    this.context = ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext();
    this.types = Types.instance(context);
    this.typeMatchVisitor = new TypeMatchVisitor(typeFactory);
    this.polyTypeMatchVisitor = new PolyTypeMatchVisitor(typeFactory);
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
    if (!serializer.isActive()) {
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
          ImmutableSet<Fix> fixes = handleEnhancedForLoop(tree, pair);
          if (!fixes.isEmpty()) {
            return fixes;
          }
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
        MethodTree enclosingMethod = Utility.findEnclosingNode(path, MethodTree.class);
        if (enclosingMethod == null) {
          return ImmutableSet.of();
        }
        toAnnotate = TreeUtils.elementFromDeclaration(enclosingMethod);
        break;
      case "argument":
        List<? extends ExpressionTree> args = Utility.getCallableArguments(path.getLeaf());
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
        toAnnotate = Utility.getCallableArgumentsSymbol(path.getLeaf()).get(index);
        break;
      default:
        return ImmutableSet.of();
    }
    if (toAnnotate == null) {
      return ImmutableSet.of();
    }
    TypeMatchVisitor visitor = new TypeMatchVisitor(typeFactory);
    List<List<Integer>> differences;
    try {
      // todo: for now we ignore the possible exceptions thrown by the visitor.
      differences = visitor.visit(pair.required, pair.found, null);
    } catch (Exception e) {
      return ImmutableSet.of();
    }

    if (!differences.isEmpty() && differences.get(0).equals(List.of(0))) {
      differences.remove(0);
    }
    if (differences.isEmpty()) {
      return ImmutableSet.of();
    }
    Fix fixOnLeftHandSide =
        new Fix(SymbolLocation.createLocationFromSymbol((Symbol) toAnnotate, context));
    fixOnLeftHandSide.location.setTypeVariablePositions(differences);
    return ImmutableSet.of(fixOnLeftHandSide);
  }

  private ImmutableSet<Fix> handleEnhancedForLoop(Tree tree, FoundRequired pair) {
    Element element = TreeUtils.elementFromTree(tree);
    Type type = null;
    int index = -1;
    if (element instanceof Symbol.VarSymbol) {
      type = ((Symbol.VarSymbol) element).type;
    }
    if (element instanceof Symbol.MethodSymbol) {
      type = ((Symbol.MethodSymbol) element).getReturnType();
    }
    if (type instanceof Type.TypeVar && tree instanceof JCTree.JCMethodInvocation) {
      ExpressionTree receiver = TreeUtils.getReceiverTree((ExpressionTree) tree);
      element = TreeUtils.elementFromUse(receiver);
      if (element instanceof Symbol.VarSymbol) {
        Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
        List<String> vars =
            varSymbol.type.tsym.type.getTypeArguments().stream()
                .map(t -> t.tsym.name.toString())
                .collect(Collectors.toList());
        index = vars.indexOf(type.tsym.name.toString());
        if (index != -1) {
          type = varSymbol.type.getTypeArguments().get(index);
        }
      }
    }
    if (type == null) {
      return ImmutableSet.of();
    }
    // todo: check for all subtypes.
    if (type.tsym.name.toString().equals("Array")) {
      AnnotatedTypeMirror elementAnnotatedType = typeFactory.getAnnotatedType(element);
      if (elementAnnotatedType instanceof AnnotatedTypeMirror.AnnotatedExecutableType) {
        elementAnnotatedType =
            ((AnnotatedTypeMirror.AnnotatedExecutableType) elementAnnotatedType).getReturnType();
      }
      AnnotatedTypeMirror.AnnotatedArrayType arrayType =
          (AnnotatedTypeMirror.AnnotatedArrayType) elementAnnotatedType;
      SymbolLocation location = SymbolLocation.createLocationFromSymbol((Symbol) element, context);
      if (location == null || location.path() == null) {
        return ImmutableSet.of();
      }
      location.setTypeVariablePositions(
          typeMatchVisitor.visit(arrayType.getComponentType(), pair.required, null));
      return ImmutableSet.of(new Fix(location));
    }
    final ImmutableSet<String> supportedTypes = ImmutableSet.of("List", "ArrayList", "Collection");
    if (!supportedTypes.contains(type.tsym.name.toString())) {
      return ImmutableSet.of();
    }
    List<Integer> effectiveTypeArgumentRegion = index != -1 ? List.of(index + 1, 1) : List.of(1);
    AnnotatedTypeMirror typeArgumentRegion =
        getAnnotatedTypeMirrorOfTypeArgumentAt(
            typeFactory.getAnnotatedType(element), effectiveTypeArgumentRegion);
    if (typeFactory.hasUntaintedAnnotation(pair.required)
        && !typeFactory.hasUntaintedAnnotation(typeArgumentRegion)) {
      SymbolLocation location = SymbolLocation.createLocationFromSymbol((Symbol) element, context);
      if (location == null || location.path() == null) {
        return ImmutableSet.of();
      }
      location.setTypeVariablePositions(
          List.of(index != -1 ? List.of(index + 1, 1, 0) : List.of(1, 0)));
      return ImmutableSet.of(new Fix(location));
    }
    return ImmutableSet.of();
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
        Utility.getClosestOverriddenMethod(overridingMethod, types);
    if (overriddenMethod == null) {
      return ImmutableSet.of();
    }
    int paramIndex = overridingMethod.getParameters().indexOf((Symbol.VarSymbol) treeElement);
    Symbol toBeAnnotated = overriddenMethod.getParameters().get(paramIndex);
    SymbolLocation location = SymbolLocation.createLocationFromSymbol(toBeAnnotated, context);
    if (location != null) {
      List<List<Integer>> differences = typeMatchVisitor.visit(pair.found, pair.required, null);
      if (differences.isEmpty()) {
        return ImmutableSet.of();
      }
      location.setTypeVariablePositions(typeMatchVisitor.visit(pair.found, pair.required, null));
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
        Utility.getClosestOverriddenMethod(overridingMethod, types);
    if (overriddenType == null) {
      overriddenType = typeFactory.getAnnotatedType(overriddenMethod);
    }
    AnnotatedTypeMirror overriddenReturnType = overriddenType.getReturnType();
    AnnotatedTypeMirror overridingReturnType =
        typeFactory.getAnnotatedType(overridingMethod).getReturnType();
    List<List<Integer>> differences =
        typeMatchVisitor.visit(overriddenReturnType, overridingReturnType, null);
    if (!differences.isEmpty()) {
      SymbolLocation location = SymbolLocation.createLocationFromSymbol(overriddenMethod, context);
      if (location == null) {
        return ImmutableSet.of();
      }
      location.setTypeVariablePositions(differences);
      ans.add(new Fix(location));
    }
    differences = typeMatchVisitor.visit(overridingReturnType, overriddenReturnType, null);
    if (!differences.isEmpty()) {
      SymbolLocation location = SymbolLocation.createLocationFromSymbol(overridingMethod, context);
      if (location == null) {
        return ImmutableSet.of();
      }
      location.setTypeVariablePositions(differences);
      ans.add(new Fix(location));
    } else {
      List<List<Integer>> onPoly =
          polyTypeMatchVisitor.visit(overridingReturnType, overriddenReturnType, null);
      if (!onPoly.isEmpty()) {
        Set<MethodParameterLocation> methodParameterLocations = new HashSet<>();
        MethodLocation location =
            (MethodLocation) SymbolLocation.createLocationFromSymbol(overridingMethod, context);
        if (location == null) {
          return ImmutableSet.of();
        }
        location.setTypeVariablePositions(onPoly);
        for (int i = 0;
            i < typeFactory.getAnnotatedType(overridingMethod).getParameterTypes().size();
            i++) {
          AnnotatedTypeMirror typeVariable = overriddenType.getParameterTypes().get(i);
          List<List<Integer>> differencesOnParam =
              polyTypeMatchVisitor.visit(
                  typeFactory.getAnnotatedType(overridingMethod).getParameterTypes().get(i),
                  typeVariable,
                  null);
          if (!differencesOnParam.isEmpty()) {
            MethodParameterLocation methodParameterLocation =
                (MethodParameterLocation)
                    SymbolLocation.createLocationFromSymbol(
                        overridingMethod.getParameters().get(i), context);
            if (methodParameterLocation == null) {
              continue;
            }
            methodParameterLocation.setTypeVariablePositions(differencesOnParam);
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

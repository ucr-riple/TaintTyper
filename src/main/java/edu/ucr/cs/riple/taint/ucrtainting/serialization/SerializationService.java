package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixVisitor;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.TypeMatchVisitor;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** This class is used to serialize the errors and the fixes for the errors. */
public class SerializationService {

  /** Serializer for the checker. */
  private final Serializer serializer;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if
   * the tree is {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}
   */
  private final UCRTaintingAnnotatedTypeFactory typeFactory;
  /** Using checker instance. */
  private final UCRTaintingChecker checker;
  /** Javac context instance. */
  private final Context context;

  public SerializationService(UCRTaintingChecker checker) {
    this.checker = checker;
    this.serializer = new Serializer(checker);
    this.typeFactory = (UCRTaintingAnnotatedTypeFactory) checker.getTypeFactory();
    this.context = ((JavacProcessingEnvironment) checker.getProcessingEnvironment()).getContext();
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
    Set<Fix> resolvingFixes;
    try {
      resolvingFixes =
          checkErrorIsFixable(source, messageKey)
              ? generateFixesForError((Tree) source, messageKey, pair)
              : ImmutableSet.of();
    } catch (Exception e) {
      System.err.println("Error in computing required fixes: " + source + " " + messageKey);
      resolvingFixes = ImmutableSet.of();
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
        return handleReturnOverrideError(path.getLeaf(), pair);
      default:
        ClassTree classTree = Utility.findEnclosingNode(path, ClassTree.class);
        if (classTree == null) {
          return ImmutableSet.of();
        }
        Symbol.ClassSymbol encClass =
            (Symbol.ClassSymbol) TreeUtils.elementFromDeclaration(classTree);
        if (!Utility.isInAnnotatedPackage(encClass, typeFactory)) {
          return ImmutableSet.of();
        }
        return tree.accept(new FixVisitor(context, typeFactory, pair), null);
    }
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
      location.setTypeVariablePositions(
          new TypeMatchVisitor(typeFactory).visit(pair.found, pair.required, null));
    }
    return ImmutableSet.of(new Fix("untainted", location));
  }

  /**
   * Computes the required fixes for wrong return override errors (type="override.return").
   *
   * @return the set of required fixes to resolve errors of type="override.return".
   */
  private ImmutableSet<Fix> handleReturnOverrideError(
      Tree overridingMethodTree, FoundRequired pair) {
    Symbol.MethodSymbol overridingMethod =
        (Symbol.MethodSymbol) TreeUtils.elementFromTree(overridingMethodTree);
    SymbolLocation location = SymbolLocation.createLocationFromSymbol(overridingMethod, context);
    if (location != null) {
      location.setTypeVariablePositions(
          new TypeMatchVisitor(typeFactory).visit(pair.found, pair.required, null));
    }
    return ImmutableSet.of(new Fix("untainted", location));
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
        return true;
      default:
        // TODO: investigate if there are other cases where the error is fixable.
        // For all other cases, return false.
        return false;
    }
  }
}

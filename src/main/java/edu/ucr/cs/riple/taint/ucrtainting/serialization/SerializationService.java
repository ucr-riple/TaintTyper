package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
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
   * @param args the arguments of the error message
   */
  public void serializeError(Object source, String messageKey, Object[] args) {
    if (!serializer.isActive()) {
      return;
    }
    Set<Fix> resolvingFixes =
        checkErrorIsFixable(source, messageKey)
            ? generateFixesForError((Tree) source, messageKey)
            : ImmutableSet.of();
    Error error =
        new Error(messageKey, args, resolvingFixes, checker.getVisitor().getCurrentPath());
    serializer.serializeError(error);
  }

  /**
   * Generates the fixes for the given tree if exists.
   *
   * @param tree The given tree.
   * @param messageKey The key of the error message.
   */
  public Set<Fix> generateFixesForError(Tree tree, String messageKey) {
    TreePath path = checker.getVisitor().getCurrentPath();
    switch (messageKey) {
      case "override.param":
        return handleParamOverrideError(tree);
      case "override.return":
        return handleReturnOverrideError(path.getLeaf());
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
        return new FixVisitor(context, typeFactory, tree).generateFixes();
    }
  }

  /**
   * Computes the required fixes for wrong parameter override errors (type="override.param").
   *
   * @param paramTree the parameter tree.
   * @return the set of required fixes to resolve errors of type="override.param".
   */
  private ImmutableSet<Fix> handleParamOverrideError(Tree paramTree) {
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
    return ImmutableSet.of(
        new Fix("untainted", SymbolLocation.createLocationFromSymbol(toBeAnnotated, context)));
  }

  /**
   * Computes the required fixes for wrong return override errors (type="override.return").
   *
   * @return the set of required fixes to resolve errors of type="override.return".
   */
  private ImmutableSet<Fix> handleReturnOverrideError(Tree overridingMethodTree) {
    Symbol.MethodSymbol overridingMethod =
        (Symbol.MethodSymbol) TreeUtils.elementFromTree(overridingMethodTree);
    return ImmutableSet.of(
        new Fix("untainted", SymbolLocation.createLocationFromSymbol(overridingMethod, context)));
  }

  /**
   * Checks if the error is fixable with annotation injections on the source code elements.
   *
   * @param source The source of the error.
   * @param messageKey The key of the error message.
   * @return True, if the error is fixable, false otherwise.
   */
  private static boolean checkErrorIsFixable(Object source, String messageKey) {
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
        return true;
      default:
        // TODO: investigate if there are other cases where the error is fixable.
        // For all other cases, return false.
        return false;
    }
  }
}

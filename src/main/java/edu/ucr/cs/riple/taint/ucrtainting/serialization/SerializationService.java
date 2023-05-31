package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.Config;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.TreeUtils;

/** This class is used to serialize the errors and the fixes for the errors. */
public class SerializationService {

  /** Serializer for the checker. */
  private final Serializer serializer;
  /** The configuration of the serialization. */
  private final Config config;

  public SerializationService(Config config) {
    this.serializer = new Serializer(config);
    this.config = config;
  }

  /**
   * This method is called when a warning or error is reported by the checker and serialized the
   * error along the set of required fixes to resolve the error if exists.
   *
   * @param source      the source of the error
   * @param messageKey  the key of the error message
   * @param args        the arguments of the error message
   * @param visitor     the visitor that is visiting the source
   * @param tf          the type factory of the checker
   * @param context     javac context.
   */
  public void serializeError(
          Object source,
          String messageKey,
          Object[] args,
          SourceVisitor<?, ?> visitor,
          GenericAnnotatedTypeFactory<?, ?, ?, ?> tf, Context context) {
    if (!config.serializationEnabled()) {
      return;
    }
    UCRTaintingAnnotatedTypeFactory typeFactory = (UCRTaintingAnnotatedTypeFactory) tf;
    // TODO: for TreeChecker instance below, use the actual API which checks if the tree is
    // @Tainted. For now, we pass tree -> true, to serialize a fix for all expressions on the right
    // hand side of the assignment.
    Set<Fix> resolvingFixes =
        checkErrorIsFixable(source, messageKey)
            ? generateFixesForError(
                (Tree) source, messageKey, visitor.getCurrentPath(), tree -> true, typeFactory, context)
            : ImmutableSet.of();
    Error error = new Error(messageKey, args, resolvingFixes, visitor.getCurrentPath());
    serializer.serializeError(error);
  }

  /**
   * Generates the fixes for the given tree if exists.
   *
   * @param tree The given tree.
   * @param messageKey The key of the error message.
   * @param treeChecker The tree checker to check if a tree requires a fix.
   * @param path The path of the tree.
   * @param context The javac context.
   */
  public Set<Fix> generateFixesForError(
      Tree tree, String messageKey, TreePath path, TreeChecker treeChecker, UCRTaintingAnnotatedTypeFactory typeFactory, Context context) {
    switch (messageKey) {
      case "override.param":
        return handleParamOverrideError(tree, context);
      case "override.return":
        return handleReturnOverrideError(path.getLeaf(), context);
      default:
        return new FixVisitor(treeChecker, context, typeFactory).visit(tree, null);
    }
  }

  /**
   * Computes the required fixes for wrong parameter override errors (type="override.param").
   *
   * @param paramTree the parameter tree.
   * @param context the javac context.
   * @return the set of required fixes to resolve errors of type="override.param".
   */
  private ImmutableSet<Fix> handleParamOverrideError(Tree paramTree, Context context) {
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
        new Fix(
            "untainted", SymbolLocation.createLocationFromSymbol(toBeAnnotated, context, null)));
  }

  /**
   * Computes the required fixes for wrong return override errors (type="override.return").
   *
   * @param overridingMethodTree the overriding method tree.
   * @param context the javac context.
   * @return the set of required fixes to resolve errors of type="override.return".
   */
  private ImmutableSet<Fix> handleReturnOverrideError(Tree overridingMethodTree, Context context) {
    Symbol.MethodSymbol overridingMethod =
        (Symbol.MethodSymbol) TreeUtils.elementFromTree(overridingMethodTree);
    return ImmutableSet.of(
        new Fix(
            "untainted", SymbolLocation.createLocationFromSymbol(overridingMethod, context, null)));
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

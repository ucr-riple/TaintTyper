package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static java.util.stream.Collectors.toSet;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.PolyMethodLocation;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** Fix visitor for method return statements. */
public class MethodReturnVisitor extends SpecializedFixComputer {

  /**
   * A map from method to the required fixes to make the method return type match the required type.
   */
  private final Map<Symbol.MethodSymbol, Set<Fix>> store;
  /** A map from method to the state of the method. */
  private final Map<Symbol.MethodSymbol, STATE> states;
  /** A set of invocations have been visited in the bodies invoked methods */
  private final Set<Invocation> invocations;

  /** The state of the method with respect to the fix computation. */
  public enum STATE {
    /** The method is currently being visited. */
    VISITING,
    /** The method has been visited and the required fixes have been computed. */
    VISITED,
    /** The method has not been visited. */
    NOT_VISITED
  }

  public MethodReturnVisitor(
      UCRTaintingAnnotatedTypeFactory factory, FixComputer fixComputer, Context context) {
    super(factory, fixComputer, context);
    this.store = new HashMap<>();
    this.states = new HashMap<>();
    this.invocations = new HashSet<>();
  }

  @Override
  public Set<Fix> visitMethod(MethodTree node, FoundRequired pair) {
    Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) TreeUtils.elementFromDeclaration(node);
    if (symbol == null) {
      return Set.of();
    }
    STATE state = getState(symbol);
    if (state.equals(STATE.VISITING)) {
      return Set.of();
    }
    if (state.equals(STATE.VISITED)) {
      return store.get(symbol);
    }
    this.states.put(symbol, STATE.VISITING);
    Fix onMethod = buildFixForElement(symbol, pair);
    if (onMethod == null) {
      return mergeResults(symbol, Collections.emptySet());
    }
    if (!typeFactory.polyTaintInferenceEnabled()) {
      return mergeResults(symbol, Set.of(onMethod));
    }
    // check if method is static, for non-static method we do not infer poly tainted annotations.
    if (!symbol.isStatic()) {
      return mergeResults(symbol, Set.of(onMethod));
    }
    Set<Fix> ans = new HashSet<>();
    Set<Fix> onReturns = node.accept(new ReturnStatementVisitor(pair), fixComputer);
    Deque<Fix> workList = new ArrayDeque<>(onReturns);
    Set<Symbol.VarSymbol> involvedElementsInReturnValueCreation = new HashSet<>();
    while (!workList.isEmpty()) {
      Fix fix = workList.pop();
      if (!fix.location.getKind().isLocalVariable()) {
        ans.add(fix);
      }
      Symbol involvedElement = fix.location.getTarget();
      if (!(involvedElement instanceof Symbol.VarSymbol)
          || involvedElementsInReturnValueCreation.contains(involvedElement)) {
        // already processed
        continue;
      }
      AssignmentScanner assignmentScanner =
          new AssignmentScanner(involvedElement, pair, typeFactory);
      involvedElementsInReturnValueCreation.add((Symbol.VarSymbol) involvedElement);
      pair.incrementDepth();
      Set<Fix> onAssignments = node.accept(assignmentScanner, fixComputer);
      pair.decrementDepth();
      workList.addAll(onAssignments);
    }
    Set<MethodParameterLocation> formalParametersUsedInReturnValueComputation = new HashSet<>();
    Set<Fix> nonParameterFixes = new HashSet<>();
    ans.forEach(
        fix -> {
          if (!(fix.location instanceof MethodParameterLocation)) {
            nonParameterFixes.add(fix);
            return;
          }
          MethodParameterLocation param = (MethodParameterLocation) fix.location;
          if (param.enclosingMethod.equals(symbol)) {
            // makes this method polymorphic.
            formalParametersUsedInReturnValueComputation.add(param);
          } else {
            // irrelevant to this method
            nonParameterFixes.add(fix);
          }
        });
    if (formalParametersUsedInReturnValueComputation.isEmpty()) {
      return mergeResults(symbol, Set.of(onMethod));
    }
    Fix polymorphicFixOnMethod =
        new Fix(
                new PolyMethodLocation(
                    (MethodLocation) onMethod.location,
                    formalParametersUsedInReturnValueComputation))
            .toPoly();
    nonParameterFixes.add(polymorphicFixOnMethod);
    nonParameterFixes.addAll(
        computeFixesForArgumentsOnInferredPolyTaintedMethods(
            (PolyMethodLocation) polymorphicFixOnMethod.location, pair));
    return mergeResults(symbol, nonParameterFixes);
  }

  /**
   * Get the state of the method.
   *
   * @param symbol The method symbol.
   * @return The state of the method.
   */
  public STATE getState(Symbol.MethodSymbol symbol) {
    return this.states.getOrDefault(symbol, STATE.NOT_VISITED);
  }

  /**
   * Adds an invocation that has been visited throughout the fix computation.
   *
   * @param node The invocation node.
   * @param pair The required type.
   */
  public void addInvocation(MethodInvocationTree node, FoundRequired pair) {
    this.invocations.add(new Invocation(node, pair));
  }

  /**
   * Computes the fixes for the arguments of the inferred poly tainted methods for the invocations
   * observed throughout the fix computation.
   *
   * @param polyMethodLocation The poly method location.
   * @param pair The required type.
   * @return The fixes for the arguments of the inferred poly tainted methods.
   */
  public Set<Fix> computeFixesForArgumentsOnInferredPolyTaintedMethods(
      PolyMethodLocation polyMethodLocation, FoundRequired pair) {
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) polyMethodLocation.target;
    Set<Invocation> processedInvocations = new HashSet<>();
    Set<Fix> ans = new HashSet<>();
    Set<Invocation> invocations = new HashSet<>(this.invocations);
    invocations.forEach(
        invocation -> {
          if (invocation.calledMethod.equals(methodSymbol)) {
            JCTree.JCMethodDecl decl =
                (JCTree.JCMethodDecl) Utility.locateDeclaration(methodSymbol, context);
            if (decl == null) {
              return;
            }
            polyMethodLocation.arguments.forEach(
                parameter -> {
                  int index = parameter.index;
                  AnnotatedTypeMirror formalParameterAnnotatedTypeMirror =
                      typeFactory.getAnnotatedType(decl.getParameters().get(index)).deepCopy(true);
                  typeFactory.makeUntainted(
                      formalParameterAnnotatedTypeMirror, parameter.typeIndexSet);
                  AnnotatedTypeMirror foundParameterType =
                      typeFactory.getAnnotatedType(invocation.node.getArguments().get(index));
                  Set<Fix> passedArgumentOnInvocation =
                      invocation
                          .node
                          .getArguments()
                          .get(index)
                          .accept(
                              fixComputer,
                              FoundRequired.of(
                                  foundParameterType,
                                  formalParameterAnnotatedTypeMirror,
                                  pair.depth));
                  ans.addAll(passedArgumentOnInvocation);
                });
            processedInvocations.add(invocation);
          }
        });
    this.invocations.removeAll(processedInvocations);
    return ans;
  }

  private Set<Fix> mergeResults(Symbol.MethodSymbol symbol, Set<Fix> fixes) {
    fixes = new HashSet<>(fixes);
    Set<PolyMethodLocation> inferredPolyMethods =
        fixes.stream()
            .filter(Fix::isPoly)
            .map(fix -> (PolyMethodLocation) fix.location)
            .collect(toSet());
    Set<Fix> toRemove = new HashSet<>();
    fixes.stream()
        .filter(fix -> fix.location.getKind().isParameter())
        .forEach(
            fix -> {
              MethodParameterLocation methodParameterLocation =
                  (MethodParameterLocation) fix.location;
              // check if the parameter is an inferred poly argument of a poly method
              for (PolyMethodLocation inferredPolyMethod : inferredPolyMethods) {
                if (inferredPolyMethod.target.equals(methodParameterLocation.enclosingMethod)) {
                  // we have a poly method that matches the enclosing method of this parameter
                  if (inferredPolyMethod.arguments.stream()
                      .noneMatch(
                          m ->
                              m.index == methodParameterLocation.index
                                  && m.typeIndexSet.equals(methodParameterLocation.typeIndexSet))) {
                    // is an untainted for non poly argument. should be considered a poly
                    // argument.
                    inferredPolyMethod.arguments.add(methodParameterLocation);
                  }
                  toRemove.add(fix);
                  return;
                }
              }
            });
    fixes.removeAll(toRemove);
    this.store.put(symbol, fixes);
    this.states.put(symbol, STATE.VISITED);
    return fixes;
  }

  public void reset() {
    this.store.clear();
    this.states.clear();
    this.invocations.clear();
  }

  abstract static class AccumulateScanner extends TreeScanner<Set<Fix>, FixComputer> {

    protected final FoundRequired pair;

    public AccumulateScanner(FoundRequired pair) {
      this.pair = pair;
    }

    @Override
    public Set<Fix> reduce(Set<Fix> r1, Set<Fix> r2) {
      if (r2 == null && r1 == null) {
        return Set.of();
      }
      Set<Fix> combined = new HashSet<>();
      if (r1 != null) {
        combined.addAll(r1);
      }
      if (r2 != null) {
        combined.addAll(r2);
      }
      return combined;
    }
  }

  private static class AssignmentScanner extends AccumulateScanner {

    private final Symbol variable;
    private final UCRTaintingAnnotatedTypeFactory factory;

    public AssignmentScanner(
        Symbol variable, FoundRequired pair, UCRTaintingAnnotatedTypeFactory factory) {
      super(pair);
      this.variable = variable;
      this.factory = factory;
    }

    @Override
    public Set<Fix> visitAssignment(AssignmentTree node, FixComputer visitor) {
      Element element = TreeUtils.elementFromUse(node.getVariable());
      if (variable.equals(element)) {
        if (node.getVariable().getKind().equals(Tree.Kind.ARRAY_ACCESS)) {
          if (pair.required instanceof AnnotatedTypeMirror.AnnotatedArrayType
              && pair.found instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
            AnnotatedTypeMirror.AnnotatedArrayType required =
                (AnnotatedTypeMirror.AnnotatedArrayType) pair.required;
            AnnotatedTypeMirror found = factory.getAnnotatedType(node.getExpression());
            FoundRequired newPair =
                FoundRequired.of(found, required.getComponentType(), pair.depth);
            return node.getExpression().accept(visitor, newPair);
          }
        }
        return node.getExpression().accept(visitor, pair);
      }
      return Set.of();
    }

    @Override
    public Set<Fix> visitVariable(VariableTree node, FixComputer visitor) {
      Element element = TreeUtils.elementFromDeclaration(node);
      if (variable.equals(element)) {
        if (node.getInitializer() == null) {
          return Set.of();
        }
        FoundRequired newPair =
            FoundRequired.of(
                factory.getAnnotatedType(node.getInitializer()), pair.required, pair.depth);
        return node.getInitializer().accept(visitor, newPair);
      }
      return Set.of();
    }
  }

  private static class ReturnStatementVisitor extends AccumulateScanner {

    public ReturnStatementVisitor(FoundRequired pair) {
      super(pair);
    }

    @Override
    public Set<Fix> visitReturn(ReturnTree node, FixComputer visitor) {
      return node.getExpression().accept(visitor, pair);
    }
  }

  private static class Invocation {

    public final MethodInvocationTree node;
    public final FoundRequired pair;
    public final Symbol.MethodSymbol calledMethod;

    public Invocation(MethodInvocationTree node, FoundRequired pair) {
      this.node = node;
      this.pair = pair;
      this.calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Invocation)) {
        return false;
      }
      Invocation that = (Invocation) o;
      return Objects.equals(node, that.node)
          && Objects.equals(pair, that.pair)
          && Objects.equals(calledMethod, that.calledMethod);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, pair, calledMethod);
    }

    @Override
    public String toString() {
      return node.toString();
    }
  }
}

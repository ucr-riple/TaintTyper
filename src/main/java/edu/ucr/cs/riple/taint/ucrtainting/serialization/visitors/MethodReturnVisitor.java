/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import static java.util.stream.Collectors.toSet;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.PolyMethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners.AssignmentScanner;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners.ReturnStatementScanner;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    Set<Fix> onReturns = node.accept(new ReturnStatementScanner(pair), fixComputer);
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
                (JCTree.JCMethodDecl) SymbolUtils.locateDeclaration(methodSymbol, context);
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

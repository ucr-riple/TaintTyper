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

import com.sun.source.tree.IdentifierTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners.AssignmentScanner;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

public class LocalVariableFixVisitor extends SpecializedFixComputer {

  private final Map<Symbol.VarSymbol, Set<Fix>> cache;
  private final Set<Symbol.VarSymbol> visiting;

  public LocalVariableFixVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    super(typeFactory, fixComputer, context);
    this.cache = new HashMap<>();
    this.visiting = new HashSet<>();
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromTree(node);
    if (!(element instanceof Symbol.VarSymbol)) {
      return Set.of();
    }
    Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
    List<Pair<Symbol.VarSymbol, FoundRequired>> localVariables = new ArrayList<>();
    localVariables.add(Pair.of(varSymbol, pair));
    Set<Fix> fixes = new HashSet<>();
    while (!localVariables.stream().allMatch(p -> visited(p.fst))) {
      Set<Pair<Symbol.VarSymbol, FoundRequired>> toProcess = new HashSet<>();
      for (Pair<Symbol.VarSymbol, FoundRequired> p : localVariables) {
        if (visited(p.fst)) {
          continue;
        }
        Set<Fix> newFixes = computeAndUpdateCache(p.fst, p.snd);
        fixes.addAll(newFixes);
        newFixes.forEach(
            fix -> {
              if (fix.location.getKind().isLocalVariable()) {
                Symbol.VarSymbol varSymbol1 = (Symbol.VarSymbol) fix.location.getTarget();
                FoundRequired newPair =
                    typeFactory.makeUntaintedPair(
                        varSymbol1, fix.location.getTypeIndexSet(), pair.depth);
                toProcess.add(Pair.of(varSymbol1, newPair));
              }
            });
      }
      localVariables.addAll(toProcess);
    }
    return fixes;
  }

  /**
   * Reset the cache of fixes. Required to be called before visiting a new series of fix
   * computations for a new error.
   */
  public void reset() {
    cache.clear();
  }

  /**
   * Check if a local variable has been visited or is currently being visited. If the local variable
   * is being visited, we should not visit it again to avoid infinite loops.
   *
   * @param varSymbol The local variable to check.
   * @return True if the local variable has been visited or is currently being visited.
   */
  private boolean visited(Symbol.VarSymbol varSymbol) {
    return cache.containsKey(varSymbol) || visiting.contains(varSymbol);
  }

  /**
   * Get the fixes for a local variable from the cache.
   *
   * @param varSymbol The local variable to get the fixes for.
   * @return The set of fixes for the local variable.
   */
  private Set<Fix> get(Symbol.VarSymbol varSymbol) {
    if (visiting.contains(varSymbol)) {
      return Set.of();
    }
    return cache.get(varSymbol);
  }

  /**
   * Computes the fixes for a local variable and updates the cache.
   *
   * @param varSymbol The local variable to compute the fixes for.
   * @param pair The required and found annotations.
   * @return The set of fixes for the local variable.
   */
  private Set<Fix> computeAndUpdateCache(Symbol.VarSymbol varSymbol, FoundRequired pair) {
    if (visited(varSymbol)) {
      return get(varSymbol);
    }
    visiting.add(varSymbol);
    // compute
    Set<Fix> fixes = computeFixesForVariable(varSymbol, pair);
    cache.put(varSymbol, fixes);
    visiting.remove(varSymbol);
    return fixes;
  }

  /**
   * Computes the fixes on every assignment to a local variable.
   *
   * @param varSymbol The local variable to compute the fixes for.
   * @param pair The required and found annotations.
   * @return The set of fixes for the local variable.
   */
  private Set<Fix> computeFixesForVariable(Symbol.VarSymbol varSymbol, FoundRequired pair) {
    Fix onLocalVariable = buildFixForElement(varSymbol, pair);
    if (onLocalVariable == null) {
      return Set.of();
    }
    try {
      JCTree declarationTree = SymbolUtils.locateDeclaration(varSymbol.owner, context);
      if (!(declarationTree instanceof JCTree.JCMethodDecl)) {
        return Set.of(onLocalVariable);
      }
      JCTree.JCMethodDecl enclosingMethod = (JCTree.JCMethodDecl) declarationTree;
      FoundRequired assignmentRequiredPair =
          typeFactory.makeUntaintedPair(
              varSymbol, onLocalVariable.location.getTypeIndexSet(), pair.depth);
      AssignmentScanner assignmentScanner =
          new AssignmentScanner(varSymbol, assignmentRequiredPair, typeFactory);
      Set<Fix> fixes = enclosingMethod.accept(assignmentScanner, fixComputer);
      if (fixes == null || fixes.isEmpty()) {
        return Set.of(onLocalVariable);
      }
      Set<Fix> newFixes = new HashSet<>(fixes);
      newFixes.add(onLocalVariable);
      return newFixes;
    } catch (Exception e) {
      return Set.of(onLocalVariable);
    }
  }
}

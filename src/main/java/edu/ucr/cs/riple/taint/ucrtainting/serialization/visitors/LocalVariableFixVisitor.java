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

  private final Map<Pair<Symbol.VarSymbol, FoundRequired>, Set<Fix>> cache;
  private final Set<Pair<Symbol.VarSymbol, FoundRequired>> visiting;

  public LocalVariableFixVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    super(typeFactory, fixComputer, context);
    this.cache = new HashMap<>();
    this.visiting = new HashSet<>();
  }

  private boolean visited(Symbol.VarSymbol varSymbol, FoundRequired foundRequired) {
    return cache.containsKey(Pair.of(varSymbol, foundRequired))
        || visiting.contains(Pair.of(varSymbol, foundRequired));
  }

  private Set<Fix> get(Symbol.VarSymbol varSymbol, FoundRequired foundRequired) {
    if (visiting.contains(Pair.of(varSymbol, foundRequired))) {
      return Set.of();
    }
    return cache.get(Pair.of(varSymbol, foundRequired));
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
    while (!localVariables.stream().allMatch(p -> visited(p.fst, p.snd))) {
      Set<Pair<Symbol.VarSymbol, FoundRequired>> toProcess = new HashSet<>();
      for (Pair<Symbol.VarSymbol, FoundRequired> p : localVariables) {
        if (visited(p.fst, p.snd)) {
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

  private Set<Fix> computeAndUpdateCache(Symbol.VarSymbol varSymbol, FoundRequired pair) {
    if (visited(varSymbol, pair)) {
      return get(varSymbol, pair);
    }
    visiting.add(Pair.of(varSymbol, pair));
    // compute
    Set<Fix> fixes = computeFixesForVariable(varSymbol, pair);
    cache.put(Pair.of(varSymbol, pair), fixes);
    visiting.remove(Pair.of(varSymbol, pair));
    return fixes;
  }

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

  public void reset() {
    cache.clear();
  }
}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class LocalVariableFixVisitor extends SpecializedFixComputer {

  private final Map<Pair<Symbol.VarSymbol, FoundRequired>, Set<Fix>> cache;
  FixComputer fixComputer;

  public LocalVariableFixVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    super(typeFactory, fixComputer, context);
    this.cache = new HashMap<>();
  }

  private boolean visited(Symbol.VarSymbol varSymbol, FoundRequired foundRequired) {
    return cache.containsKey(Pair.of(varSymbol, foundRequired));
  }

  private Set<Fix> get(Symbol.VarSymbol varSymbol, FoundRequired foundRequired) {
    return cache.get(Pair.of(varSymbol, foundRequired));
  }

  @Override
  public Set<Fix> visitIdentifier(IdentifierTree node, FoundRequired pair) {
    Element element = TreeUtils.elementFromTree(node);
    if (!(element instanceof Symbol.VarSymbol)) {
      return Set.of();
    }
    Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
    if (visited(varSymbol, pair)) {
      return get(varSymbol, pair);
    }
    // compute
    Set<Fix> fixes = computeFixesForVariable(varSymbol, pair);
    cache.put(Pair.of(varSymbol, pair), fixes);
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
      AnnotatedTypeMirror localVarType = typeFactory.getAnnotatedType(varSymbol).deepCopy(true);
      typeFactory.makeUntainted(localVarType, onLocalVariable.location.getTypeIndexSet());
      FoundRequired localVarPair =
          FoundRequired.of(typeFactory.getAnnotatedType(varSymbol), localVarType, pair.depth);
      AssignmentScanner assignmentScanner =
          new AssignmentScanner(varSymbol, localVarPair, typeFactory);
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

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.LocalVariableLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.PolyMethodLocation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.TreeUtils;

/** Fix visitor for method return statements. */
public class MethodReturnVisitor extends BasicVisitor {

  private final AccumulateScanner returnStatementScanner;

  public MethodReturnVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    super(context, factory, pair);
    this.returnStatementScanner =
        new AccumulateScanner() {
          @Override
          public Set<Fix> visitReturn(ReturnTree node, FixVisitor visitor) {
            return node.getExpression().accept(visitor, null);
          }
        };
  }

  @Override
  public Set<Fix> visitMethod(MethodTree node, Void unused) {
    Element methodElement = TreeUtils.elementFromDeclaration(node);
    Fix onMethod = buildFixForElement(methodElement);
    if (onMethod == null) {
      return Set.of();
    }
    Set<Fix> ans = new HashSet<>();
    Set<Fix> onReturns =
        node.accept(returnStatementScanner, new FixVisitor(context, typeFactory, pair));
    Deque<Fix> workList = new ArrayDeque<>(onReturns);
    while (!workList.isEmpty()) {
      Fix fix = workList.pop();
      if (!fix.location.getKind().isLocalVariable()) {
        ans.add(fix);
      } else {
        AssignmentScanner assignmentScanner =
            new AssignmentScanner((Symbol.VarSymbol) ((LocalVariableLocation) fix.location).target);
        Set<Fix> onAssignments =
            node.accept(assignmentScanner, new FixVisitor(context, typeFactory, pair));
        workList.addAll(onAssignments);
      }
    }
    Set<MethodParameterLocation> polymorphicAnnotations = new HashSet<>();
    Set<Fix> others = new HashSet<>();
    ans.forEach(
        fix -> {
          if (!(fix.location instanceof MethodParameterLocation)) {
            others.add(fix);
            return;
          }
          MethodParameterLocation param = (MethodParameterLocation) fix.location;
          if (param.enclosingMethod.equals(methodElement)) {
            polymorphicAnnotations.add(param);
          } else {
            others.add(fix);
          }
        });
    if (polymorphicAnnotations.isEmpty()) {
      return Set.of(onMethod);
    }
    Fix polymorphicFixOnMethod =
        new Fix(new PolyMethodLocation((MethodLocation) onMethod.location, polymorphicAnnotations))
            .toPoly();
    others.add(polymorphicFixOnMethod);
    return others;
  }

  abstract static class AccumulateScanner extends TreeScanner<Set<Fix>, FixVisitor> {
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

  static class AssignmentScanner extends AccumulateScanner {

    private final Symbol.VarSymbol localVariable;

    public AssignmentScanner(Symbol.VarSymbol localVariable) {
      this.localVariable = localVariable;
    }

    @Override
    public Set<Fix> visitAssignment(AssignmentTree node, FixVisitor visitor) {
      Element element = TreeUtils.elementFromUse(node.getVariable());
      if (localVariable.equals(element)) {
        return node.getExpression().accept(visitor, null);
      }
      return Set.of();
    }

    @Override
    public Set<Fix> visitVariable(VariableTree node, FixVisitor visitor) {
      Element element = TreeUtils.elementFromDeclaration(node);
      if (localVariable.equals(element)) {
        return node.getInitializer().accept(visitor, null);
      }
      return Set.of();
    }
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.LocalVariableLocation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.checkerframework.javacutil.TreeUtils;

/** Fix visitor for method return statements. */
public class MethodReturnVisitor extends SimpleTreeVisitor<Set<Fix>, Void> {

  /** The javac context. */
  protected final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  @Nullable protected final FoundRequired pair;

  private final TreeScanner<Set<Fix>, FixVisitor> returnStatementScanner;

  public MethodReturnVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory factory, FoundRequired pair) {
    this.context = context;
    this.typeFactory = factory;
    this.pair = pair;
    this.returnStatementScanner =
        new TreeScanner<>() {

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

          @Override
          public Set<Fix> visitReturn(ReturnTree node, FixVisitor visitor) {
            return node.getExpression().accept(visitor, null);
          }
        };
  }

  @Override
  public Set<Fix> visitMethod(MethodTree node, Void unused) {
    Set<Fix> ans = new HashSet<>();
    Set<Fix> onReturns =
        node.accept(returnStatementScanner, new FixVisitor(context, typeFactory, pair));
    Deque<Fix> workList = new ArrayDeque<>(onReturns);
    while (!workList.isEmpty()) {
      Fix fix = workList.pop();
      if (!fix.location.getKind().equals(ElementKind.LOCAL_VARIABLE)) {
        ans.add(fix);
      } else {
        AssignmentVisitor assignmentVisitor =
            new AssignmentVisitor((Symbol.VarSymbol) ((LocalVariableLocation) fix.location).target);
        Set<Fix> onAssignments =
            node.accept(assignmentVisitor, new FixVisitor(context, typeFactory, pair));
        workList.addAll(onAssignments);
      }
    }
    return ans;
  }

  static class AssignmentVisitor extends TreeScanner<Set<Fix>, FixVisitor> {

    private final Symbol.VarSymbol localVariable;

    public AssignmentVisitor(Symbol.VarSymbol localVariable) {
      this.localVariable = localVariable;
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

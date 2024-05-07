package edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixComputer;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class AssignmentScanner extends AccumulateScanner {

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
          FoundRequired newPair = FoundRequired.of(found, required.getComponentType(), pair.depth);
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

  @Override
  public Set<Fix> visitEnhancedForLoop(EnhancedForLoopTree node, FixComputer fixComputer) {
    // todo handle enhanced for loop
    return super.visitEnhancedForLoop(node, fixComputer);
  }
}

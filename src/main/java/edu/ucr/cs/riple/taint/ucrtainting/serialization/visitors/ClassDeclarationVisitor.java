package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.Tree;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.Set;

public class ClassDeclarationVisitor extends SpecializedFixComputer {

  public ClassDeclarationVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory,
      FixComputer fixComputer,
      UCRTaintingChecker checker) {
    super(typeFactory, fixComputer, checker);
  }

  public Set<Fix> compute(Tree node, FoundRequired pair) {
    throw new RuntimeException("Not implemented");
    //        return Set.of();
  }
}

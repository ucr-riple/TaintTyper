package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.Set;

public class ClassDeclarationVisitor extends SpecializedFixComputer {

  public ClassDeclarationVisitor(
      Context context, UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer) {
    super(context, typeFactory, fixComputer);
  }

  public Set<Fix> compute(Tree node, FoundRequired pair) {
    throw new RuntimeException("Not implemented");
    //        return Set.of();
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners;

import com.sun.source.tree.ReturnTree;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixComputer;
import java.util.Set;

public class ReturnStatementScanner extends AccumulateScanner {

  public ReturnStatementScanner(FoundRequired pair) {
    super(pair);
  }

  @Override
  public Set<Fix> visitReturn(ReturnTree node, FixComputer visitor) {
    return node.getExpression().accept(visitor, pair);
  }
}

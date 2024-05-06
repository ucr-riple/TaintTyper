package edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners;

import com.sun.source.util.TreeScanner;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixComputer;
import java.util.HashSet;
import java.util.Set;

abstract class AccumulateScanner extends TreeScanner<Set<Fix>, FixComputer> {

  protected final FoundRequired pair;

  public AccumulateScanner(FoundRequired pair) {
    this.pair = pair;
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
}

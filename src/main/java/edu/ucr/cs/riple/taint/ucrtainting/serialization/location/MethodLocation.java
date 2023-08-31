package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  public final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target, JCTree declarationTree) {
    super(LocationKind.METHOD, target, declarationTree);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitMethod(this, p);
  }
}

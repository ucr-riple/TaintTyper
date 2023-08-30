package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting class fields. */
public class FieldLocation extends AbstractSymbolLocation {

  /** Symbol of targeted class field */
  public final Symbol.VarSymbol variableSymbol;

  public FieldLocation(Symbol target, JCTree declarationTree) {
    super(ElementKind.FIELD, target, declarationTree);
    variableSymbol = (Symbol.VarSymbol) target;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitField(this, p);
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  public LocalVariableLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.LOCAL_VARIABLE, target, declarationTree, type);
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
  }
}

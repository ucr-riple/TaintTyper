package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.List;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  public final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.METHOD, target, declarationTree, type);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }

  @Override
  public List<Type> getTypeVariables() {
    return ((Symbol.MethodSymbol) target).getReturnType().tsym.type.getTypeArguments();
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitMethod(this, p);
  }
}

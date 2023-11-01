package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;
import org.checkerframework.javacutil.TreeUtils;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  public final Symbol.MethodSymbol enclosingMethod;

  public LocalVariableLocation(Symbol target, JCTree declarationTree) {
    super(LocationKind.LOCAL_VARIABLE, target);
    // TODO: Rewrite the enclosing method. This is a hack to make the serialization work for now.
    this.enclosingMethod =
        (Symbol.MethodSymbol)
            ((Symbol) Objects.requireNonNull(TreeUtils.elementFromTree(declarationTree))).owner;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocalVariableLocation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    LocalVariableLocation that = (LocalVariableLocation) o;
    return Objects.equals(enclosingMethod, that.enclosingMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enclosingMethod);
  }
}

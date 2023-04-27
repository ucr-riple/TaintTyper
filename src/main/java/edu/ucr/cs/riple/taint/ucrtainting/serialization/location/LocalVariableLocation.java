package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import org.checkerframework.javacutil.TreeUtils;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  public final Symbol.MethodSymbol enclosingMethod;

  public LocalVariableLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.LOCAL_VARIABLE, target, declarationTree, type);
    // TODO: Rewrite the enclosing method. This is a hack to make the serialization work for now.
    this.enclosingMethod =
        (Symbol.MethodSymbol)
            ((Symbol) Objects.requireNonNull(TreeUtils.elementFromTree(declarationTree))).owner;
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitLocalVariable(this, p);
  }
}

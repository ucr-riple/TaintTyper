package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import java.util.List;
import javax.lang.model.element.ElementKind;
import org.json.JSONObject;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  protected final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.METHOD, target, declarationTree, type);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }

  @Override
  protected List<Type> getTypeVariables() {
    return declarationTree != null
        ? ((JCTree.JCMethodDecl) declarationTree).getReturnType().type.tsym.type.getTypeArguments()
        : List.of();
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = super.toJSON();
    ans.put("method", Serializer.serializeSymbol(this.enclosingMethod));
    return ans;
  }
}

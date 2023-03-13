package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import javax.lang.model.element.ElementKind;
import org.json.JSONObject;

/** subtype of {@link AbstractSymbolLocation} targeting methods. */
public class MethodLocation extends AbstractSymbolLocation {

  /** Symbol of the targeted method. */
  protected final Symbol.MethodSymbol enclosingMethod;

  public MethodLocation(Symbol target) {
    super(ElementKind.METHOD, target);
    enclosingMethod = (Symbol.MethodSymbol) target;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = super.toJSON();
    ans.put("method", Serializer.serializeSymbol(this.enclosingMethod));
    return ans;
  }
}

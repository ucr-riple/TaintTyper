package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import javax.lang.model.element.ElementKind;
import org.json.JSONObject;

/** subtype of {@link AbstractSymbolLocation} targeting class fields. */
public class FieldLocation extends AbstractSymbolLocation {

  /** Symbol of targeted class field */
  protected final Symbol.VarSymbol variableSymbol;

  public FieldLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.FIELD, target, declarationTree, type);
    variableSymbol = (Symbol.VarSymbol) target;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = super.toJSON();
    ans.put("field", Serializer.serializeSymbol(variableSymbol));
    return ans;
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import javax.lang.model.element.ElementKind;
import org.json.JSONObject;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  public LocalVariableLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.LOCAL_VARIABLE, target, declarationTree, type);
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = super.toJSON();
    ans.put("varName", Serializer.serializeSymbol(target));
    return ans;
  }
}

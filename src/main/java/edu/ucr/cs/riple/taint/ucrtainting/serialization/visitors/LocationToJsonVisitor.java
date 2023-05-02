package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.AbstractSymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.FieldLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.LocalVariableLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

public class LocationToJsonVisitor implements LocationVisitor<JSONObject, Void> {

  private JSONObject defaultAction(AbstractSymbolLocation location) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("path", location.path != null ? location.path.toString() : "null");
    jsonObject.put("kind", location.kind.name());
    jsonObject.put("class", Serializer.serializeSymbol(location.enclosingClass));
    jsonObject.put(
        "pos", location.declarationTree != null ? location.declarationTree.getStartPosition() : -1);
    jsonObject.put(
        "type-variables",
        new JSONArray(location.getTypeVariables().stream().map(Objects::toString).toArray()));
    jsonObject.put(
        "target-type", location.targetType != null ? location.targetType.toString() : "null");
    return jsonObject;
  }

  @Override
  public JSONObject visitMethod(MethodLocation method, Void unused) {
    JSONObject ans = defaultAction(method);
    ans.put("method", Serializer.serializeSymbol(method.enclosingMethod));
    return ans;
  }

  @Override
  public JSONObject visitField(FieldLocation field, Void unused) {
    JSONObject ans = defaultAction(field);
    ans.put("field", Serializer.serializeSymbol(field.variableSymbol));
    return ans;
  }

  @Override
  public JSONObject visitParameter(MethodParameterLocation parameter, Void unused) {
    JSONObject ans = defaultAction(parameter);
    ans.put("method", Serializer.serializeSymbol(parameter.enclosingMethod));
    ans.put("index", parameter.index);
    ans.put("name", Serializer.serializeSymbol(parameter.paramSymbol));
    return ans;
  }

  @Override
  public JSONObject visitLocalVariable(LocalVariableLocation localVariable, Void unused) {
    JSONObject ans = defaultAction(localVariable);
    ans.put("varName", Serializer.serializeSymbol(localVariable.target));
    ans.put("method", Serializer.serializeSymbol(localVariable.enclosingMethod));
    return ans;
  }
}

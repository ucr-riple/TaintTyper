package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.AbstractSymbolLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.FieldLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.LocalVariableLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.MethodParameterLocation;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.PolyMethodLocation;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Visitor for converting a location to a JSON object. */
public class LocationToJsonVisitor implements LocationVisitor<JSONObject, Void> {

  private JSONObject defaultAction(AbstractSymbolLocation location) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("path", location.path != null ? location.path.toString() : "null");
    jsonObject.put("kind", location.kind.name());
    jsonObject.put("class", Serializer.serializeSymbol(location.enclosingClass));
    JSONArray typeVariablePositions = new JSONArray();
    location.typeVariablePositions.forEach(
        integers -> {
          JSONArray positions = new JSONArray(integers);
          typeVariablePositions.put(positions);
        });
    jsonObject.put("type-variable-position", typeVariablePositions);
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

  @Override
  public JSONObject visitPolyMethod(PolyMethodLocation polyMethodLocation, Void unused) {
    JSONObject ans = defaultAction(polyMethodLocation);
    ans.put("method", Serializer.serializeSymbol(polyMethodLocation.target));
    ans.put(
        "arguments",
        new JSONArray(
            polyMethodLocation.arguments.stream()
                .map(methodParameterLocation -> methodParameterLocation.index)
                .collect(Collectors.toSet())));
    return ans;
  }
}

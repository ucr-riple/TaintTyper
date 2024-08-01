/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors;

import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.Serializer;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.TypeIndex;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.AbstractSymbolLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.ClassDeclarationLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.FieldLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.LocalVariableLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.MethodLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.MethodParameterLocation;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.location.PolyMethodLocation;
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
    location.typeIndexSet.forEach(
        typeIndex -> {
          JSONArray positions = new JSONArray(typeIndex.getContent());
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
    JSONObject arguments = new JSONObject();
    polyMethodLocation.arguments.forEach(
        methodParameterLocation -> {
          JSONArray positions =
              new JSONArray(
                  methodParameterLocation.typeIndexSet.stream()
                      .map(TypeIndex::getContent)
                      .collect(Collectors.toSet()));
          arguments.put(String.valueOf(methodParameterLocation.index), positions);
        });
    ans.put("arguments", arguments);
    return ans;
  }

  @Override
  public JSONObject visitClassDeclaration(ClassDeclarationLocation location, Void unused) {
    JSONObject ans = defaultAction(location);
    ans.put("target", location.toChange.tsym.toString());
    return ans;
  }
}

package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Map;

public class TypeArgumentRegion {
  public final List<Integer> effectiveRegionIndex;
  public final Map<String, String> typeVarMap;
  public final Type type;

  public TypeArgumentRegion(
      List<Integer> effectiveRegionIndex, Map<String, String> typeVarMap, Type type) {
    this.effectiveRegionIndex = effectiveRegionIndex;
    this.typeVarMap = typeVarMap;
    this.type = type;
  }

  @Override
  public String toString() {
    return "EffectiveRegion{"
        + "effectiveRegionIndex="
        + effectiveRegionIndex
        + ", typeVarMap="
        + typeVarMap
        + ", type="
        + type
        + '}';
  }
}

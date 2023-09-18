package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Map;

/** Denotes a region in a type. */
public class TypeArgumentRegion {

  /** Indices which denotes the region on a give type. */
  public final List<Integer> index;
  /** Map of type variables to their corresponding type variables. */
  public final Map<String, String> typeVarMap;
  /** The actual confined in by the indices. */
  public final Type type;

  public TypeArgumentRegion(List<Integer> index, Map<String, String> typeVarMap, Type type) {
    this.index = index;
    this.typeVarMap = typeVarMap;
    this.type = type;
  }
}

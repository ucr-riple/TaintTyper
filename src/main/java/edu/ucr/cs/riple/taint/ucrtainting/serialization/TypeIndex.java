package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TypeIndex {

  private final List<Integer> content;

  public static final TypeIndex TOP_LEVEL = TypeIndex.of(0);

  public TypeIndex() {
    this.content = new ArrayList<>();
  }

  public TypeIndex(TypeIndex other) {
    this.content = new ArrayList<>(other.content);
  }

  public static TypeIndex of(int... indexes) {
    TypeIndex typeIndex = new TypeIndex();
    for (int index : indexes) {
      typeIndex.content.add(index);
    }
    return typeIndex;
  }

  public static TypeIndex of(List<Integer> indexes) {
    TypeIndex typeIndex = new TypeIndex();
    typeIndex.content.addAll(indexes);
    return typeIndex;
  }

  public static Set<TypeIndex> setOf(int... indexes) {
    Set<TypeIndex> set = new HashSet<>();
    set.add(TypeIndex.of(indexes));
    return set;
  }

  public static Set<TypeIndex> setOf(TypeIndex index) {
    Set<TypeIndex> set = new HashSet<>();
    set.add(index);
    return set;
  }

  public static Set<TypeIndex> topLevel() {
    return setOf(TOP_LEVEL);
  }

  public TypeIndex relativeTo(TypeIndex other) {
    TypeIndex relative = new TypeIndex();
    relative.content.addAll(other.content);
    relative.content.addAll(this.content);
    return relative;
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }

  public Integer poll() {
    return content.remove(0);
  }

  public int size() {
    return content.size();
  }

  public Integer get(int index) {
    return content.get(index);
  }

  public ImmutableList<Integer> getContent() {
    return ImmutableList.copyOf(content);
  }

  public TypeIndex copy() {
    return new TypeIndex(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypeIndex)) {
      return false;
    }
    TypeIndex typeIndex = (TypeIndex) o;
    return Objects.equals(getContent(), typeIndex.getContent());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getContent());
  }
}

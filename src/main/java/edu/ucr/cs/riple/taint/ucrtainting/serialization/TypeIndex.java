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

package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Represents a type index in a type hierarchy. */
public class TypeIndex {

  /** List of indices to the target type. */
  private final List<Integer> content;

  /** Top level type index. This index will target the top level type in the hierarchy. */
  public static final TypeIndex TOP_LEVEL = TypeIndex.of(0);

  public TypeIndex() {
    this.content = new ArrayList<>();
  }

  public TypeIndex(TypeIndex other) {
    this.content = new ArrayList<>(other.content);
  }

  /**
   * Creates a new type index with the given indexes.
   *
   * @param indexes indexes to the target type
   * @return a new type index
   */
  public static TypeIndex of(int... indexes) {
    TypeIndex typeIndex = new TypeIndex();
    for (int index : indexes) {
      typeIndex.content.add(index);
    }
    return typeIndex;
  }

  /**
   * Creates a new type index with the given indexes.
   *
   * @param indexes indexes to the target type
   * @return a new type index
   */
  public static TypeIndex of(List<Integer> indexes) {
    TypeIndex typeIndex = new TypeIndex();
    typeIndex.content.addAll(indexes);
    return typeIndex;
  }

  /**
   * Creates a new set of type indexes with the given indexes.
   *
   * @param indexes indexes to the target type
   * @return a new set of type indexes
   */
  public static Set<TypeIndex> setOf(int... indexes) {
    Set<TypeIndex> set = new HashSet<>();
    set.add(TypeIndex.of(indexes));
    return set;
  }

  /**
   * Creates a new set of type indexes with the given type index.
   *
   * @param index type index
   * @return a new set of type indexes
   */
  public static Set<TypeIndex> setOf(TypeIndex index) {
    Set<TypeIndex> set = new HashSet<>();
    set.add(index);
    return set;
  }

  /**
   * Returns the top level type index.
   *
   * @return the top level type index
   */
  public static Set<TypeIndex> topLevel() {
    return setOf(TOP_LEVEL);
  }

  /**
   * Creates a new type index which is relative to the given type index. (e.g. given type index is
   * [2, 1] and this type index is [3, 4], the relative type index will be [2, 1, 3, 4])
   *
   * @param other Type index to be relative to
   * @return a new type index which is relative to the given type index
   */
  public TypeIndex relativeTo(TypeIndex other) {
    TypeIndex relative = new TypeIndex();
    relative.content.addAll(other.content);
    relative.content.addAll(this.content);
    return relative;
  }

  /**
   * Checks if this type index is empty.
   *
   * @return true if this type index is empty, false otherwise
   */
  public boolean isEmpty() {
    return content.isEmpty();
  }

  /**
   * Removes the first index from the type index and returns it.
   *
   * @return the first index
   */
  public Integer poll() {
    return content.remove(0);
  }

  /**
   * Returns the size of the type index.
   *
   * @return the size of the type index
   */
  public int size() {
    return content.size();
  }

  /**
   * Returns the index at the given position.
   *
   * @param index position of the index
   * @return the index at the given position
   */
  public Integer get(int index) {
    return content.get(index);
  }

  /**
   * Returns the content of the type index.
   *
   * @return ImmutableList of the content of the type index.
   */
  public ImmutableList<Integer> getContent() {
    return ImmutableList.copyOf(content);
  }

  /**
   * Returns a copy of this type index.
   *
   * @return a copy of this type index
   */
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

  @Override
  public String toString() {
    // for purposes of debugging
    return content.toString();
  }
}

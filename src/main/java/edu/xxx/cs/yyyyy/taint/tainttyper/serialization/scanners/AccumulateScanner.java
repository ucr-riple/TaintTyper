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

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization.scanners;

import com.sun.source.util.TreeScanner;
import edu.xxx.cs.yyyyy.taint.tainttyper.FoundRequired;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.Fix;
import edu.xxx.cs.yyyyy.taint.tainttyper.serialization.visitors.FixComputer;
import java.util.HashSet;
import java.util.Set;

/** A scanner that accumulates the results of visiting a tree. */
abstract class AccumulateScanner extends TreeScanner<Set<Fix>, FixComputer> {

  /** The pair of required and found annotations. */
  protected final FoundRequired pair;

  public AccumulateScanner(FoundRequired pair) {
    this.pair = pair;
  }

  @Override
  public Set<Fix> reduce(Set<Fix> r1, Set<Fix> r2) {
    if (r2 == null && r1 == null) {
      return Set.of();
    }
    Set<Fix> combined = new HashSet<>();
    if (r1 != null) {
      combined.addAll(r1);
    }
    if (r2 != null) {
      combined.addAll(r2);
    }
    return combined;
  }
}

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

package edu.xxx.cs.yyyyy.taint.tainttyper.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.xxx.cs.yyyyy.taint.tainttyper.util.SymbolUtils;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

/** Represents the reporting error from the checker. */
public class Error implements JSONSerializable {

  /** Message key for the error. */
  public final String messageKey;
  /**
   * Set of fixes that can resolve the error. If the error is not fixable, this set will be empty.
   */
  public final ImmutableSet<Fix> resolvingFixes;
  /**
   * The class symbol of the region that contains the error. If the error is not in a region, this
   * will be null.
   */
  @Nullable public final Symbol.ClassSymbol regionClass;
  /**
   * The symbol of the region member that contains the error. If the error is not in a class, or
   * inside a static initializer block, this will be null.
   */
  @Nullable public final Symbol regionSymbol;
  /** Offset of program point where this error is reported. */
  public final int offset;
  /** Path to the source file where this error is reported. */
  public final Path path;

  public Error(String messageKey, Set<Fix> resolvingFixes, TreePath path) {
    this.messageKey = messageKey;
    this.resolvingFixes =
        resolvingFixes == null ? ImmutableSet.of() : ImmutableSet.copyOf(resolvingFixes);
    this.regionClass = SymbolUtils.findRegionClassSymbol(path);
    this.regionSymbol = SymbolUtils.findRegionMemberSymbol(this.regionClass, path);
    this.offset = ((JCTree) path.getLeaf()).getStartPosition();
    this.path =
        Serializer.pathToSourceFileFromURI(path.getCompilationUnit().getSourceFile().toUri());
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = new JSONObject();
    ans.put("offset", offset);
    ans.put("path", path.toString());
    ans.put("messageKey", messageKey);
    JSONObject region = new JSONObject();
    region.put("class", Serializer.serializeSymbol(regionClass));
    region.put("symbol", Serializer.serializeSymbol(regionSymbol));
    ans.put("region", region);
    ans.put("fixes", new JSONArray(this.resolvingFixes.stream().map(Fix::toJSON).toArray()));
    return ans;
  }
}

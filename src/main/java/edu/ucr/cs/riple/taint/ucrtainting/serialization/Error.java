package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
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

  public final FoundRequired pair;

  public Error(String messageKey, Set<Fix> resolvingFixes, TreePath path, FoundRequired pair) {
    this.messageKey = messageKey;
    this.resolvingFixes =
        resolvingFixes == null ? ImmutableSet.of() : ImmutableSet.copyOf(resolvingFixes);
    this.regionClass = Utility.findRegionClassSymbol(path);
    this.regionSymbol = Utility.findRegionMemberSymbol(this.regionClass, path);
    this.offset = ((JCTree) path.getLeaf()).getStartPosition();
    this.pair = pair;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject ans = new JSONObject();
    ans.put("offset", offset);
    ans.put("messageKey", messageKey);
    JSONObject region = new JSONObject();
    region.put("class", Serializer.serializeSymbol(regionClass));
    region.put("symbol", Serializer.serializeSymbol(regionSymbol));
    ans.put("region", region);
    ans.put("fixes", new JSONArray(this.resolvingFixes.stream().map(Fix::toJSON).toArray()));
    JSONObject pair = new JSONObject();
    pair.put("found", this.pair.found.toString());
    pair.put("required", this.pair.required.toString());
    ans.put("pair", pair);
    return ans;
  }
}

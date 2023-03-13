package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import java.util.Set;
import javax.annotation.Nullable;

/** Represents the reporting error from the checker. */
// todo: contents of this class will be finalized once the final format is determined.
public class Error {
  /** Message key for the error. */
  public final String messageKey;
  /** Message for the error. */
  public final String message;
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

  public Error(String messageKey, String message, Set<Fix> resolvingFixes, TreePath path) {
    this.messageKey = messageKey;
    this.message = message;
    this.resolvingFixes = ImmutableSet.copyOf(resolvingFixes);
    this.regionClass = Utility.findRegionClassSymbol(path);
    this.regionSymbol = Utility.findRegionMemberSymbol(this.regionClass, path);
  }
}

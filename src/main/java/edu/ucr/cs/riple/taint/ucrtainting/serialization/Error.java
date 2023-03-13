package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import com.google.common.collect.ImmutableSet;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import java.util.Set;

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

  public final Symbol.ClassSymbol regionClass;
  public final Symbol regionSymbol;

  public Error(String messageKey, String message, Set<Fix> resolvingFixes, TreePath path) {
    this.messageKey = messageKey;
    this.message = message;
    this.resolvingFixes = ImmutableSet.copyOf(resolvingFixes);
  }
}

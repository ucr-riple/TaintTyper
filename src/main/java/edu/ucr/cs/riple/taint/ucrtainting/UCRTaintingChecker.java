package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This is the entry point for pluggable type-checking.
 */
@StubFiles({
        "Connection.astub",
        "General.astub",
        "ApacheCommonsIO.astub",
        "ApacheCommonsLang.astub"
})
public class UCRTaintingChecker extends BaseTypeChecker {}

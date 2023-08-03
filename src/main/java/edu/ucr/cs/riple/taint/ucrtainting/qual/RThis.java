package edu.ucr.cs.riple.taint.ucrtainting.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a possibly-tainted value: at run time, the value might be tainted or might be untainted.
 *
 * @see RUntainted
 * @see edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({RTainted.class})
public @interface RThis {}

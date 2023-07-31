package edu.ucr.cs.riple.taint.ucrtainting.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a reference that is possibly validated
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(RTainted.class)
public @interface RPossiblyValidated {
  public String[] value() default {};
}

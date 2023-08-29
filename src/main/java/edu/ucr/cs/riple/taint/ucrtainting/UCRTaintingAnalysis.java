package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.common.accumulation.AccumulationAnalysis;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class UCRTaintingAnalysis extends AccumulationAnalysis {
    /**
     * Constructs an AccumulationAnalysis.
     *
     * @param checker the checker
     * @param factory the type factory
     */
    public UCRTaintingAnalysis(BaseTypeChecker checker, UCRTaintingAnnotatedTypeFactory factory) {
        super(checker, factory);
    }
}

package com.imminentmeals.prestige.codegen;

import java.util.Arrays;

import javax.annotation.processing.Processor;

/**
 * Test utilities.
 * @author Dandré
 */
/* package */final class ProcessorTestUtilities {
    /* package */static final int ONE = 1;
    /* package */static final int TWO = 2;
    /* package */static final int THREE = 3;
    /* package */static final int FOUR = 4;
    /* package */static final int FIVE = 5;
    /* package */static final int SIX = 6;
    /* package */static final int SEVEN = 7;

    /* package */static Iterable<? extends Processor> prestigeProcessors() {
        return Arrays.asList(
                new AnnotationProcessor()
        );
    }

/* Private constructor */
    private ProcessorTestUtilities() { }
}
